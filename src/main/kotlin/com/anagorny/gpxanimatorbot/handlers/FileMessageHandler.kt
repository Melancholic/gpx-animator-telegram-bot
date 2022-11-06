package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.helpers.*
import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.model.OutputFormats
import com.anagorny.gpxanimatorbot.services.ForecastService
import com.anagorny.gpxanimatorbot.services.GpxProcessor
import com.anagorny.gpxanimatorbot.services.MainTelegramBotService
import com.anagorny.gpxanimatorbot.services.RateLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KLogging
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.time.Duration
import java.util.*
import kotlin.math.max

@Component
class FileMessageHandler(
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val systemProperties: SystemProperties,
    private val rateLimiter: RateLimiter,
    private val botService: MainTelegramBotService,
    @Qualifier("mainFlowCoroutineScope")
    private val scope: CoroutineScope,
    private val gpxProcessor: GpxProcessor,
    private val forecastService: ForecastService
) : UpdatesHandler {


    override suspend fun handle(update: Update) = withContext(MDCContext()) {
        val message = update.message
        val document = message.document

        if (rlChecking(message) && validateInputFile(message)) {

            var file: File? = null
            var outFile: File? = null

            try {
                botService.sentAction(message.chatId, ActionType.TYPING)
                file = botService.downloadFile(botService.execute(GetFile(document.fileId)))

                val messageWithForecast = doForecast(file, message).orElse(null)

                val combinedResult = gpxProcessor.doProcess(file)
                outFile = combinedResult.second

                botService.sentAction(message.chatId, ActionType.UPLOADVIDEO)
                botService.execute(buildResponse(message, document, combinedResult.first, outFile))
                deleteMessage(messageWithForecast)
            } catch (e: Exception) {
                logger.error(e) { "Error while processing message" }
            } finally {
                scope.launchAsync(Dispatchers.IO) { removeFileIfExist(file?.absolutePath, MainHandler.logger) }
                scope.launchAsync(Dispatchers.IO) { removeFileIfExist(outFile?.absolutePath, MainHandler.logger) }
            }
        }
    }

    // ToDo create as helper's method
    private fun deleteMessage(message: Message?) {
        if (message != null) {
            botService.execute(DeleteMessage().apply {
                chatId = message.chatId.toString()
                messageId = message.messageId
            })
            logger.info("Message with id=${message.messageId} was deleted")
        }
    }

    private suspend fun doForecast(file: File, message: Message): Optional<Message> {
        val forecastDuration = runBlocking { forecastService.forecast(file) }
        return if (forecastDuration.orElse(Duration.ZERO).toSeconds() > 30) {
            val duration = forecastDuration.get()
                doResponse(
                    "Your request is processing. " +
                            "Your GPX file may takes ${duration.format()} to process." +
                            " Stay in touch \uD83D\uDE42",
                    message
                ).asOptional()
        } else {
            Optional.empty()
        }
    }

    private suspend fun rlChecking(message: Message): Boolean {
        if (rateLimiter.isRateLimitingEnabled()) {
            val rlKey = message.from.id.toString()
            if (!rateLimiter.isRequestAllowed(rlKey)) {
                val mins = max(rateLimiter.howLongForAllow(rlKey).toMinutes(), 1)
                doResponse(
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
            doResponse("Your file isn't GPX (attachment must have '.gpx' extension)", message)
            return false
        }
        if (document.fileSize > systemProperties.inputFileMaxSize.toBytes()) {
            doResponse(
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

    private suspend fun doResponse(response: String, userMessage: Message): Message {
        val message = SendMessage().apply {
            replyToMessageId = userMessage.messageId
            chatId = userMessage.chatId.toString()
            text = response
        }
        return botService.execute(message)
    }

    companion object : KLogging()
}
