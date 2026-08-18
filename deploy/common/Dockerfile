FROM bellsoft/liberica-openjre-alpine:26

ARG JAR_FILE
ARG ENTRY_POINT
ARG GPX_FILE_FOR_FORECAST
# Single source of truth for the GPX Animator CLI version - both docker-compose
# files and the CI build inherit it from here.
ARG GPX_ANIMATOR_VER=1.8.2
ARG WORK_DIR=/var/gpx-animator-telegram-bot

# tzdata: musl ships no zoneinfo db, so TZ (Europe/Moscow) is a no-op without it
# ca-certificates: HTTPS to the Telegram API, photon.komoot.io and the map tile server
# fontconfig/ttf-dejavu: GPX Animator renders text into the video via AWT
# wget: GNU rather than busybox's, whose HTTPS support depends on ssl_client being
#       present in the base image - the jar download below must not be a coin flip
RUN apk add --no-cache tzdata ca-certificates fontconfig ttf-dejavu wget \
    && fc-cache --force

ENV LANG=en_US.utf8

RUN addgroup -S -g 1000 app \
    && adduser -S -u 1000 -G app -h /app app

# Pre-created so the bot can write logs even when the host dir isn't
# bind-mounted; with a mount, host ownership wins and must match uid 1000.
RUN mkdir -p "$WORK_DIR/logs" && chown -R app:app "$WORK_DIR"

WORKDIR /app
# Fetched at image build time rather than shipped in the distrib: the fat jar is ~270MB.
# Pulled from GitHub Releases, which keeps every version, rather than from
# download.gpx-animator.app, which only ever serves the current one (1.8.0 and 1.8.1 are
# already 404 there). Tag == version from 1.8.1 on; the 1.8.0 asset lives under `v1.8`.
# The jar stays root-owned - mode 644 is all `java -jar` needs, and chowning 270MB
# into a second layer would double the image size.
RUN set -eux; \
    wget -O gpx-animator-app.jar \
      "https://github.com/gpx-animator/gpx-animator/releases/download/v${GPX_ANIMATOR_VER}/gpx-animator-${GPX_ANIMATOR_VER}-all.jar"; \
    java -jar gpx-animator-app.jar --version

COPY --chown=app:app ${JAR_FILE} gpx-animator-telegram-bot.jar
COPY --chown=app:app ${ENTRY_POINT} entry_point.sh
COPY --chown=app:app ${GPX_FILE_FOR_FORECAST} gpx_for_forecast.gpx

USER app

ENTRYPOINT ["sh", "./entry_point.sh"]
