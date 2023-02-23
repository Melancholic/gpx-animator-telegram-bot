package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.helpers.measureTimeMillis
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import jakarta.annotation.PostConstruct
import mu.KLogging
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


@Service
class GpxAnimatorRunnerImpl(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val telegramProperties: TelegramProperties
    ) : GpxAnimatorRunner {
    private val locker = ReentrantLock()

    private val defaultArgs =
        arrayOf("java", "-Duser.country=US", "-Duser.language=en", "-jar", gpxAnimatorAppProperties.path)

    @PostConstruct
    fun postConstruct() {
        logger.info { "GpxAnimatorRunner is initializing..." }
        runTest()
        logger.info { "GpxAnimatorRunner was initialized successful." }
    }


    override suspend fun run(inFilePath: String, outFilePath: String): File = locker.withLock {
        val process = runaGpxAnimatorProcess(
                "--input", inFilePath,
                "--output", outFilePath,
                "--tms-url-template", "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={zoom}",
                "--height", "${gpxAnimatorAppProperties.outHeight}",
                "--width", "${gpxAnimatorAppProperties.outWidth}",
                "--attribution", "Created by GPX Animator,\n via @${telegramProperties.bot.name}",
                "--background-map-visibility", "${gpxAnimatorAppProperties.backgroundMapVisibility}",
                "--fps", "${gpxAnimatorAppProperties.fps}",
                "--track-icon", "bicycle",
                "--color", "#FF0000", "--color", "#0000FF",
                "--split-multi-tracks"
            )

        val (time, executedSuccess) = measureTimeMillis {
            process.waitFor(
                gpxAnimatorAppProperties.executionTimeout.toSeconds(),
                TimeUnit.SECONDS
            )
        }

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

    override fun runTest() = locker.withLock {
        val res = runaGpxAnimatorProcess("--version").waitFor()
        if (res == 0) {
            logger.info("GPX-animator was found and returned successful code (0)")
        } else {
            throw IllegalStateException("GPX-animator return unsuccessful exit code ($res)")
        }
    }

    protected fun runaGpxAnimatorProcess(vararg args: String) : Process {
        val process = ProcessBuilder()
            .inheritIO()
            .command(*defaultArgs, *args)
            .start()
        logger.info { "GPX-animator is running with PID = ${process.pid()}..." }
        return process
    }


    companion object : KLogging()
}
