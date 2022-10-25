package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.utils.StreamGobbler
import mu.KLogging
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer


@Service
class GpxAnimatorRunnerImpl(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val telegramProperties: TelegramProperties,
    private val threadPoolTaskExecutor: AsyncListenableTaskExecutor
) : GpxAnimatorRunner {
    private val tag = "GRP-ANIMATOR-APP"
    private val locker = ReentrantLock()


    override fun run(inFilePath: String, outFilePath: String): File {
        locker.lock()
        try {
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
            inheritIO(process.inputStream, process.pid(), logger::info)
            inheritIO(process.errorStream, process.pid(), logger::error)

            val res = process.waitFor()
            logger.info("path: $inFilePath")
            if (res != 0) {
                throw IllegalStateException("GPX-animator return unsuccessful exit code ($res)")
            }
            val outFile = File(outFilePath)
            if (outFile.exists()) {
                logger.info("File '$outFilePath' was created")
                return outFile
            } else {
                throw IllegalStateException("Output file '$outFile' isn't exist")
            }
        } finally {
            locker.unlock()
        }
    }

    private fun inheritIO(stream: InputStream, pid: Long, loggerConsumer: Consumer<String?>) {
        val gobbler = StreamGobbler(stream) { line: String? -> loggerConsumer.accept("[$tag-$pid] $line") }
        threadPoolTaskExecutor.execute(gobbler)
    }

    override fun runTest() {
        locker.lock()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("java", "-jar", gpxAnimatorAppProperties.path, "--version"))
            inheritIO(process.inputStream, process.pid(), logger::info)
            inheritIO(process.errorStream, process.pid(), logger::error)
            val res = process.waitFor()
            if (res == 0) {
                logger.info("GPX-animator was found and returned success ($res)")
            } else {
                throw IllegalStateException("GPX-animator return unsuccessful exit code ($res)")
            }
        } finally {
            locker.unlock()
        }
    }

    companion object : KLogging()
}