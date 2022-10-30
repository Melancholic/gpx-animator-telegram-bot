package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.helpers.loadFile
import com.anagorny.gpxanimatorbot.helpers.makeCaption
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

        if (!rlChecking(message) || !validateInputFile(message)) return

        var file: File? = null
        var result: File? = null

        try {
            botService.sentAction(message.chatId, ActionType.TYPING)

            //ToDo async
            file = botService.downloadFile(botService.execute(GetFile(document.fileId)))
            var gpxAnalyzeResult: GPXAnalyzeResult? = null
            try {
                gpxAnalyzeResult = gpxAnalyzeService.doAnalyze(file)
            } catch (e: java.lang.Exception) {
                logger.error("Error while analyzing .GPX file", e)
            }

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
        gpxAnalyzeResult: GPXAnalyzeResult?,
        result: File
    ): SendVideo =
        SendVideo().apply {
            chatId = message.chatId.toString()
            replyToMessageId = message.messageId
            video = loadFile(result, makeOutFilename(document.fileName, gpxAnimatorAppProperties.outputFormat), logger)
            width = gpxAnimatorAppProperties.outWidth
            height = gpxAnimatorAppProperties.outHeight
            caption = makeCaption(gpxAnalyzeResult, FilenameUtils.getBaseName(document.fileName))
            parseMode = "HTML"
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