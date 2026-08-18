# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Kotlin/Spring Boot 4 Telegram bot (Java 25, Kotlin 2.4, telegrambots 10.x): users send a `.gpx` attachment, the bot
renders it into an MP4 route animation (by shelling out to the external
[GPX Animator](https://github.com/gpx-animator/gpx-animator) CLI jar) and replies with the video plus a stats caption.

## Commands

```bash
./gradlew build            # compile + `distrib` (build.dependsOn(distrib))
./gradlew bootRun          # run locally (see "Running locally" below)
./gradlew distrib          # build/distributions/<name>-<version>.zip: bootJar + deploy/common + docker-compose + env
./build-snapshot.sh        # clean build distrib devSnapshot  (nebula: 0.x.y-dev.N.<sha>)
./build-release.sh         # clean build distrib final        (nebula: tags vX.Y.Z)
```

Versions come from `nebula.release` + git tags — never hand-edit a version in `build.gradle`.

There is **no `src/test`** — the project has no tests and no test dependencies. `./gradlew test` is a no-op. If adding
tests, the test source set and `spring-boot-starter-test` must be added first. A context-load test also needs a stub
for `telegramBotInitializer`, which otherwise calls the real Telegram API at startup and fails with 401.

## Running locally

The build targets JDK 25 via a Gradle toolchain, so a JDK 25 must be installed (or resolvable by toolchain
auto-provisioning). Beyond that, two prerequisites:

1. **The GPX Animator CLI jar must exist at the path in `gpx-animator-app.path` (default `gpx-animator-app.jar`,
   relative to the process working dir).** `GpxAnimatorRunnerImpl.postConstruct` runs `java -jar <path> --version` and
   throws on failure, so **the app will not start without it**. The repo ships `gpx-animator-1.8.0-SNAPSHOT-all.jar`;
   copy/symlink it to `gpx-animator-app.jar`.
2. Required env vars: `TG_BOT_TOKEN`, `TG_BOT_NAME`, `TG_CHANNEL_ID`, `WORK_DIR` (log dir), and
   `GPX_FILE_FOR_FORECAST` when forecast is on.

Use `--spring.profiles.active=localhost` for development: it disables rate limiting and the forecast warm-up (the
warm-up renders a full reference video at startup, which is slow).

Real credentials live in `deploy/{snapshot,production}/ENV_INIT*.env`, which are gitignored — don't commit them or
inline their values.

## Architecture

**Update flow.** `MainTelegramBotService` extends `CommandLongPollingTelegramBot` and implements
`SpringLongPollingBot`, so `telegrambots-springboot-longpolling-starter` registers it automatically. Slash commands (`/start`, `/help`,
`/stop` in `commands/`) are dispatched by the framework; everything else lands in `processNonCommandUpdate`, which
launches a coroutine on `mainFlowCoroutineScope`, sets an MDC `correlationId` of `<chatId>-<messageId>`, and calls
`MainHandler`. `MainHandler` splits on `message.hasDocument()`: `FileMessageHandler` (the real work) vs.
`OtherMessageHandler` (deletes the message).

**`FileMessageHandler`** is the whole pipeline: rate-limit check → validate extension/size → download from Telegram →
optional "this will take ~N" forecast message → `GpxProcessor.doProcess` → send `SendVideo` with an HTML caption →
delete the forecast message → delete temp files asynchronously in `finally`.

**`GpxProcessor.doProcess`** fans out two coroutines and awaits both: `GPXAnalyzeService.doAnalyze` (stats) and
`GpxAnimatorRunner.run` (video). Note `GpxAnimatorRunnerImpl.run` holds a process-wide `ReentrantLock` for the entire
subprocess lifetime, so **renders are globally serialized regardless of the coroutine pool size** — the pool only
parallelizes analysis. The subprocess is killed after `gpx-animator-app.executionTimeout`; its stdout/stderr are piped
to the logger on the separate `loggingProcessCoroutineScope` (`SupervisorJob`, so a gobbler failure can't cancel the
main scope).

**Analysis** (`GPXAnalyzeServiceImpl`) uses the jpx library over all track segments: distance/elevation via
`Geoid.WGS84`, speeds via `GeoHelper` (prefers GPX-recorded `speed`, otherwise derives per-minute speeds), and
start/middle/end place names via `GeocoderClient` — a `RestTemplate` client (`clients/impl/GeocoderClientImpl`)
against photon.komoot.io that retries per `retryer.*` and falls back to formatted coordinates on any error. `MessageHelper.makeCaption` renders the result as the Telegram HTML caption.

**Forecast** estimates processing time by linearly scaling a startup benchmark: at boot `ForecastServiceImpl` renders
the reference GPX at `forecast.testGpxPath` and records duration + point count, then extrapolates by point count. It is
`@ConditionalOnProperty("forecast.enabled")`; `ForecastStubServiceImpl` (`@ConditionalOnMissingBean(name = ["forecastService"])`)
takes over when disabled and always returns `Optional.empty()`. A forecast message is only sent if the estimate exceeds
30s.

**Rate limiting** is per Telegram user id, bucket4j, in-memory `ConcurrentHashMap` — so limits reset on restart and are
not shared across instances. Limits are a list of `requests`/`period` pairs under `system.rate-limiting.limits`.

**Config.** All settings are `@ConfigurationProperties` data classes in `config/Properties.kt`, bound from
`application.yml` and scanned via `@ConfigurationPropertiesScan`. Add new settings there rather than injecting
`@Value`.

### Conventions

- Interface in `services/`, implementation in `services/impl/` — keep new services matching that split.
- Handlers implement the `suspend fun handle(update: Update)` interface in `handlers/UpdatesHandler.kt`; note
  `FileMessageHandler`/`OtherMessageHandler` are injected into `MainHandler` **by parameter name**, since both are
  `UpdatesHandler` beans.
- Coroutine scopes are injected by `@Qualifier` (`mainFlowCoroutineScope`, `loggingProcessCoroutineScope`) and defined
  in `SpringConfiguration`. Use the `runAsync`/`launchAsync`/`io` helpers in `helpers/CommonHelper.kt` — they attach
  `MDCContext` so the correlation id survives across threads.
- Logging via `io.github.oshai.kotlinlogging.KotlinLogging` (`companion object { val logger = KotlinLogging.logger {} }`);
  message arguments are lambdas — `logger.info { "..." }`, not `logger.info("...")`.
- Telegram API calls go through the injected `TelegramClient` bean (`OkHttpTelegramClient`, built in
  `SpringConfiguration`), not through the bot service. `helpers/TelegramHelper.kt` holds the `sentAction`/`deleteMessage`
  extensions on it.
- There is no web server: the app depends on `spring-boot-starter` + `spring-boot-restclient`, not `-starter-web`.

## Deployment

`distrib` produces a zip containing the boot jar, `deploy/common/*` (Dockerfile, entry point, reference GPX) and the
`docker-compose.yml` + `credentials/*.env` of the matching build type — `production` when the version matches
`\d+\.\d+\.\d+`, otherwise `snapshot`. The Dockerfile downloads the ~270MB GPX Animator jar at image build time from
the `GPX_ANIMATOR_VER` build arg, which defaults in the Dockerfile itself — bump the animator version there, in one
place. It pulls from **GitHub Releases, not `download.gpx-animator.app`**: that host only ever serves the current
release, so any pinned version 404s the build the moment upstream ships a new one (which is exactly how 1.8.0 broke).
The container runs as uid 1000 with a small heap (`-Xmx128m`), so rendering work must stay in the subprocess and a
bind-mounted `WORK_DIR` on the host must be writable by uid 1000.

`.github/workflows/docker-publish.yml` is the cloud path: every push to `main` runs `./gradlew final` (nebula tags the
release), builds `deploy/common/Dockerfile` with `context: .` and pushes to `ghcr.io/<owner>/<repo>` tagged `latest`,
`X.Y.Z`, `X.Y` and `sha-...`. Pull requests build the image but don't push. `.dockerignore` is an allowlist that only
matters for that build — local `docker compose build` runs inside the unpacked distrib dir instead.
