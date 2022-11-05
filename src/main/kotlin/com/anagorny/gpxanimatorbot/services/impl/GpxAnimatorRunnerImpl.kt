package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.helpers.measureTimeMillis
import com.anagorny.gpxanimatorbot.helpers.runAsync
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.utils.StreamGobbler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@Service
class GpxAnimatorRunnerImpl(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val telegramProperties: TelegramProperties,
    @Qualifier("loggingProcessCoroutineScope")
    private val scope: CoroutineScope
) : GpxAnimatorRunner {
    private val tag = "GRP-ANIMATOR-APP"
    private val locker = ReentrantLock()


    override suspend fun run(inFilePath: String, outFilePath: String): File = locker.withLock {
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
            )
            .start()
        val (infoJob, errorJob) = pipeIOtoLogger(process)
        logger.info { "GPX-animator is running with PID = ${process.pid()}..." }

        val (time, executedSuccess) = measureTimeMillis {
            process.waitFor(
                gpxAnimatorAppProperties.executionTimeout.toSeconds(),
                TimeUnit.SECONDS
            )
        }
        runBlocking { awaitAll(infoJob, errorJob) }

        if (!executedSuccess) {
            process.destroyForcibly()
            logger.info {
                "GPX-animator with PID = ${process.pid()} was forcibly killed after timeout in " +
                        "${gpxAnimatorAppProperties.executionTimeout.toSeconds()} seconds..."
            }
            throw TimeoutException("GPX-animator was forcibly killed after timeout")
        }
        logger.info {
            "GPX-animator with PID=${process.pid()} was finished with " +
                    "code=${process.exitValue()}, elapsed time=${formatDurationHMS(time)}"
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

    private fun pipeIOtoLogger(process: Process): Pair<Deferred<Unit>, Deferred<Unit>> {
        val infoJob =
            scope.runAsync {
                StreamGobbler(process.inputStream) { line: String? -> logger.info("[$tag-${process.pid()}] $line") }.run()
            }
        val errorJob =
            scope.runAsync {
                StreamGobbler(process.errorStream) { line: String? -> logger.error("[$tag-${process.pid()}] $line") }.run()
            }
        return (infoJob to errorJob)
    }

    override fun runTest() = locker.withLock {
        val process = Runtime.getRuntime().exec(arrayOf("java", "-jar", gpxAnimatorAppProperties.path, "--version"))
        pipeIOtoLogger(process)
        val res = process.waitFor()
        if (res == 0) {
            logger.info("GPX-animator was found and returned successful code (0)")
        } else {
            throw IllegalStateException("GPX-animator return unsuccessful exit code ($res)")
        }
    }

    companion object : KLogging()
}
