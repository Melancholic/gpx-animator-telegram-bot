package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.helpers.launchAsync
import com.anagorny.gpxanimatorbot.helpers.measureTimeMillis
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.utils.StreamGobbler
import kotlinx.coroutines.CoroutineScope
import mu.KLogging
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock


@Service
class GpxAnimatorRunnerImpl(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val telegramProperties: TelegramProperties,
    private val scope: CoroutineScope
) : GpxAnimatorRunner {
    private val tag = "GRP-ANIMATOR-APP"
    private val locker = ReentrantLock()


    override fun run(inFilePath: String, outFilePath: String): File = locker.withLock {
        val process = ProcessBuilder()
            .command(
                "java", "-jar", gpxAnimatorAppProperties.path,
                "--input", inFilePath,
                "--output", outFilePath,
                "--tms-url-template", "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={zoom}",
                "--height", "${gpxAnimatorAppProperties.outHeight}",
                "--width", "${gpxAnimatorAppProperties.outWidth}",
                "--attribution", "Created by GPX Animator,\n via @${telegramProperties.bot.name}",
                "--background-map-visibility", "${gpxAnimatorAppProperties.backgroundMapVisibility}",
                "--fps", "${gpxAnimatorAppProperties.fps}",
                "--track-icon", "bicycle"
//                    "--information-position", "'bottom left'"
            )
            .start()

        connectLogger(process)

        logger.info { "GPX-animator is running with PID = ${process.pid()}..." }
        val (time, res) = measureTimeMillis {
            process.waitFor(
                gpxAnimatorAppProperties.executionTimeout.toSeconds(),
                TimeUnit.SECONDS
            )
        }
        logger.info {
            "GPX-animator with PID = ${process.pid()} was finished, elapsed time = ${
                formatDurationHMS(
                    time
                )
            }"
        }
        if (!res) {
            process.destroyForcibly()
            logger.info { "GPX-animator with PID = ${process.pid()} was forcibly killed after timeout in ${gpxAnimatorAppProperties.executionTimeout.toSeconds()} seconds..." }
            throw TimeoutException("GPX-animator was forcibly killed after timeout in ${gpxAnimatorAppProperties.executionTimeout.toSeconds()} seconds")
        }
        if (process.exitValue() != 0) {
            throw IllegalStateException("GPX-animator return unsuccessful exit code (${process.exitValue()})")
        }
        val outFile = File(outFilePath)
        if (outFile.exists()) {
            logger.info("File '$outFilePath' was created")
            return@withLock outFile
        } else {
            throw IllegalStateException("Output file '$outFile' isn't exist")
        }
    }

    private fun connectLogger(process: Process) {
        scope.launchAsync { inheritIO(process.inputStream, process.pid(), logger::info) }
        scope.launchAsync { inheritIO(process.errorStream, process.pid(), logger::error) }
    }

    private suspend fun inheritIO(
        stream: InputStream, pid: Long,
        loggerConsumer: Consumer<String?>
    ) {
        StreamGobbler(stream) { line: String? -> loggerConsumer.accept("[$tag-$pid] $line") }.run()
    }

    override fun runTest() = locker.withLock {
        val process = Runtime.getRuntime().exec(arrayOf("java", "-jar", gpxAnimatorAppProperties.path, "--version"))
        connectLogger(process)
        val res = process.waitFor()
        if (res == 0) {
            logger.info("GPX-animator was found and returned success ($res)")
        } else {
            throw IllegalStateException("GPX-animator return unsuccessful exit code ($res)")
        }
    }

    companion object : KLogging()
}
