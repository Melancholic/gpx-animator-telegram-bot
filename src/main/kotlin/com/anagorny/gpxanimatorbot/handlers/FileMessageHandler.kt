package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.helpers.format
import com.anagorny.gpxanimatorbot.helpers.loadFile
import com.anagorny.gpxanimatorbot.helpers.removeFileIfExist
import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.model.OutputFormats
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.services.MainTelegramBotService
import com.anagorny.gpxanimatorbot.services.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KLogging
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.math.max

@Component
class FileMessageHandler(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val gpxAnimatorRunner: GpxAnimatorRunner,
    private val systemProperties: SystemProperties,
    private val rateLimiter: RateLimiter,
    private val botService: MainTelegramBotService,
    private val gpxAnalyzeService: GPXAnalyzeService
) : UpdatesHandler {


    override suspend fun handle(update: Update) {
        val message = update.message
        val document = message.document

        if (!validateInputFile(message)) return
        if (!rlChecking(message)) return

        var file: File? = null
        var result: File? = null

        try {
            file = botService.downloadFile(botService.execute(GetFile(document.fileId)))
            //ToDo async
            val gpxAnalyzeResult = gpxAnalyzeService.doAnalyze(file)
            //ToDo async
            result = gpxAnimatorRunner.run(
                file.absolutePath, withContext(Dispatchers.IO) {
                    Files.createTempFile(null, ".${gpxAnimatorAppProperties.outputFormat.ext}")
                }.absolutePathString()
            )

            botService.execute(buildResponse(message, document, gpxAnalyzeResult, result))
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception" }
        } finally {
            removeFileIfExist(file?.absolutePath, MainHandler.logger)
            removeFileIfExist(result?.absolutePath, MainHandler.logger)
        }
    }

    private suspend fun rlChecking(message: Message): Boolean {
        if (rateLimiter.isRateLimitingEnabled()) {
            val rlKey = message.from.id.toString()
            if (!rateLimiter.isRequestAllowed(rlKey)) {
                val mins = max(rateLimiter.howLongForAllow(rlKey).toMinutes(), 1)
                wrongResponse(
                    "You have exceeded the limit on requests to the bot. Try again after $mins minutes.",
                    message
                )
                return false
            }
        }
        return true
    }

    private suspend fun validateInputFile(message: Message): Boolean {
        val document = message.document
        if (!document.fileName.endsWith(".gpx")) {
            wrongResponse("Your file isn't GPX (attachment must have '.gpx' extension)", message)
            return false
        }
        if (document.fileSize > systemProperties.inputFileMaxSize.toBytes()) {
            wrongResponse(
                "Your file is larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB. Files larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB are not supported.",
                message
            )
            return false
        }
        return true
    }

    private suspend fun buildResponse(
        message: Message,
        document: Document,
        gpxAnalyzeResult: GPXAnalyzeResult,
        result: File
    ): SendVideo =
        SendVideo().apply {
            chatId = message.chatId.toString()
            replyToMessageId = message.messageId
            video = loadFile(result, makeOutFilename(document.fileName, gpxAnimatorAppProperties.outputFormat), logger)
            width = gpxAnimatorAppProperties.outWidth
            height = gpxAnimatorAppProperties.outHeight
            caption = makeCaption(document, gpxAnalyzeResult)
            parseMode = "HTML"
        }

    private suspend fun makeCaption(document: Document, gpxAnalyzeResult: GPXAnalyzeResult): String = buildString {
        append("<b>${FilenameUtils.getBaseName(document.fileName)}</b>")
        append("\n\n")
        gpxAnalyzeResult.from?.let { append("<b>From:</b> <i>$it</i> \n") }
        gpxAnalyzeResult.to?.let { append("<b>To:</b> <i>$it</i> \n") }
        gpxAnalyzeResult.duration?.let { append("<b>Duration:</b> <i>${formatDurationHMS(it.toMillis())}</i> \n") }
        gpxAnalyzeResult.distance?.let { append("<b>Distance:</b> <i>${it.format(3)} km.</i> \n") }
        gpxAnalyzeResult.avgSpeed?.let { append("<b>Avg speed:</b> <i>${it.format(2)} km/h.</i> \n") }
        gpxAnalyzeResult.maxSpeed?.let { append("<b>Max speed:</b> <i>${it.format(2)} km/h.</i> \n") }
        gpxAnalyzeResult.uphill?.let { append("<b>Uphill:</b> <i>${it.format(2)}</i> \n") }
        gpxAnalyzeResult.downhill?.let { append("<b>Downhill:</b> <i>${it.format(2)}</i>") }
    }

    private suspend fun makeOutFilename(
        sourceFilePath: String,
        extension: OutputFormats = OutputFormats.MP4
    ): String {
        val sourceFileName = FilenameUtils.getBaseName(sourceFilePath)
        return "$sourceFileName.${extension.ext}"
    }

    private suspend fun wrongResponse(response: String, userMessage: Message) {
        val message = SendMessage().apply {
            replyToMessageId = userMessage.messageId
            chatId = userMessage.chatId.toString()
            text = response
        }
        botService.execute(message)
    }

    companion object : KLogging()
}