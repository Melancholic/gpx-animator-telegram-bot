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
import org.telegram.telegrambots.meta.api.methods.ActionType
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
            botService.sentAction(message.chatId, ActionType.TYPING)

            //ToDo async
            file = botService.downloadFile(botService.execute(GetFile(document.fileId)))
            val gpxAnalyzeResult = gpxAnalyzeService.doAnalyze(file)

            //ToDo async
            botService.sentAction(message.chatId, ActionType.RECORDVIDEO)
            result = gpxAnimatorRunner.run(
                file.absolutePath, withContext(Dispatchers.IO) {
                    Files.createTempFile(null, ".${gpxAnimatorAppProperties.outputFormat.ext}")
                }.absolutePathString()
            )

            botService.sentAction(message.chatId, ActionType.UPLOADVIDEO)
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
        gpxAnalyzeResult.from?.let { append("<b>\uD83C\uDFE1 From:</b> <i>$it</i> \n") }
        gpxAnalyzeResult.to?.let { append("<b>⛰️ To:</b> <i>$it</i> \n") }
        gpxAnalyzeResult.duration?.let { append("<b>⏰ Duration:</b> <i>${formatDurationHMS(it.toMillis())}</i>\n") }
        gpxAnalyzeResult.distance?.let { append("<b>\uD83D\uDCCF Distance:</b> <i>${it.format(3)} km.</i>\n") }
        gpxAnalyzeResult.avgSpeed?.let { append("<b>\uD83D\uDE80 Avg speed:</b> <i>${it.format(2)} km/h.</i>\n") }
        gpxAnalyzeResult.maxSpeed?.let { append("<b>\uD83D\uDE80 Max speed:</b> <i>${it.format(2)} km/h.</i>\n") }
        append("\n")
        gpxAnalyzeResult.ascent.elevation.let { append("<b>↗️ Uphill:</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.ascent.totalDistance.let { append("<b>↗️ Total (distance):</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.ascent.maxDistance?.let { append("<b>↗️ Longest part (distance):</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.ascent.totalDuration.let { append("<b>↗️ Total (duration):</b> <i>${formatDurationHMS(it.toMillis())}</i>\n") }
        gpxAnalyzeResult.ascent.maxDuration?.let { append("<b>↗️ Longest part (duration):</b> <i>${formatDurationHMS(it.toMillis())}</i>\n") }
        gpxAnalyzeResult.ascent.maxAngle?.let { append("<b>↗️ Max angle:</b> <i>${it.format(2)} m.</i>\n") }
        append("\n")
        gpxAnalyzeResult.descent.elevation.let { append("<b>↘️ Downhill:</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.descent.totalDistance.let { append("<b>↘️ Total (distance):</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.descent.maxDistance?.let { append("<b>↘️ Longest part (distance):</b> <i>${it.format(2)} m.</i>\n") }
        gpxAnalyzeResult.descent.totalDuration.let { append("<b>↘️ Total (duration):</b> <i>${formatDurationHMS(it.toMillis())}</i>\n") }
        gpxAnalyzeResult.descent.maxDuration?.let { append("<b>↘️ Longest part (duration):</b> <i>${formatDurationHMS(it.toMillis())}</i>\n") }
        gpxAnalyzeResult.descent.maxAngle?.let { append("<b>↘️ Max angle:</b> <i>${it.format(2)} m.</i>\n") }
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