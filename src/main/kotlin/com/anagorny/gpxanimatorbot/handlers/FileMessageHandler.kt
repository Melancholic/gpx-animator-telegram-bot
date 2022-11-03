package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.helpers.*
import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.model.OutputFormats
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.services.MainTelegramBotService
import com.anagorny.gpxanimatorbot.services.RateLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
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
    private val gpxAnalyzeService: GPXAnalyzeService,
    private val scope: CoroutineScope
) : UpdatesHandler {


    override suspend fun handle(update: Update) {
        val message = update.message
        val document = message.document

        if (!rlChecking(message) || !validateInputFile(message)) return

        var file: File? = null
        var outFile: File? = null

        try {
            botService.sentAction(message.chatId, ActionType.TYPING)
            file = botService.downloadFile(botService.execute(GetFile(document.fileId)))

            val gpxAnalyzeResultDeferred: Deferred<GPXAnalyzeResult> =
                scope.runAsync { gpxAnalyzeService.doAnalyze(file) }

            val gpxAnimatorRunningResultDeferred: Deferred<File> = scope.runAsync {
                botService.sentAction(message.chatId, ActionType.RECORDVIDEO)
                val outFilePath = io {
                    Files.createTempFile(null, ".${gpxAnimatorAppProperties.outputFormat.ext}").absolutePathString()
                }
                gpxAnimatorRunner.run(file.absolutePath, outFilePath)

            }

            val gpxAnalyzeResult = gpxAnalyzeResultDeferred.await()
            outFile = gpxAnimatorRunningResultDeferred.await()
            botService.sentAction(message.chatId, ActionType.UPLOADVIDEO)
            botService.execute(buildResponse(message, document, gpxAnalyzeResult, outFile))
        } catch (e: Exception) {
            logger.error(e) { "Error while processing message" }
        } finally {
            scope.launchAsync(Dispatchers.IO) { removeFileIfExist(file?.absolutePath, MainHandler.logger) }
            scope.launchAsync(Dispatchers.IO) { removeFileIfExist(outFile?.absolutePath, MainHandler.logger) }
        }
    }

    private suspend fun rlChecking(message: Message): Boolean {
        if (rateLimiter.isRateLimitingEnabled()) {
            val rlKey = message.from.id.toString()
            if (!rateLimiter.isRequestAllowed(rlKey)) {
                val mins = max(rateLimiter.howLongForAllow(rlKey).toMinutes(), 1)
                wrongResponse(
                    "You have exceeded the limit on requests to the bot." +
                            " Try again after $mins minutes.", message
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
                "Your file is larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB." +
                        " Files larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB are not supported.",
                message
            )
            return false
        }
        return true
    }

    private fun buildResponse(
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


    private fun makeOutFilename(
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
