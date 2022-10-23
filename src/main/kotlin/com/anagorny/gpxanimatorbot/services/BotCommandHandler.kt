package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.helpers.loadFile
import com.anagorny.gpxanimatorbot.helpers.removeFileIfExist
import com.anagorny.gpxanimatorbot.model.OutputFormats
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Document
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File
import java.nio.file.Files
import javax.annotation.PostConstruct
import kotlin.io.path.absolutePathString


@Component
class BotCommandHandler(
    private val telegramProperties: TelegramProperties,
    commands: Set<IBotCommand>,
    private val gpxAnimatorRunner: GpxAnimatorRunner,
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
    private val systemProperties: SystemProperties
) : TelegramLongPollingCommandBot() {
    private val logger = LoggerFactory.getLogger(BotCommandHandler::class.java)

    init {
        registerAll(*commands.toTypedArray())
    }

    @PostConstruct
    protected fun postConstruct() {
        logger.info("${this.javaClass.canonicalName} was initialized")
    }

    override fun getBotUsername() = telegramProperties.bot.name
    override fun getBotToken() = telegramProperties.bot.token

    override fun processNonCommandUpdate(update: Update) {
        if (update.hasMessage()) {
            val message = update.message

            if (message.hasDocument()) {
                val document = message.document
                if (!document.fileName.endsWith(".gpx")) {
                    wrongResponse("Your file isn't GPX (attachment must have '.gpx' extension)", message)
                    return
                }
                if (document.fileSize > systemProperties.inputFileMaxSize.toBytes()) {
                    wrongResponse(
                        "Your file is larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB. Files larger than ${systemProperties.inputFileMaxSize.toMegabytes()}MB are not supported.",
                        message
                    )
                    return
                }

                var file: File? = null
                var result: File? = null

                try {
                    file = downloadFile(execute(GetFile(document.fileId)))
                    result = gpxAnimatorRunner.run(
                        file.absolutePath,
                        Files.createTempFile(null, ".${gpxAnimatorAppProperties.outputFormat.ext}").absolutePathString()
                    )

                    execute(buildResponse(message, document, result))
                } finally {
                    removeFileIfExist(file?.absolutePath, logger)
                    removeFileIfExist(result?.absolutePath, logger)
                }
            } else {
                logger.info("Unknown message id=${message.messageId}, text: '${message.text}'")
                execute(DeleteMessage().apply {
                    chatId = message.chatId.toString()
                    messageId = message.messageId
                })
                logger.info("Message with id=${message.messageId} was deleted")
            }
        }
    }

    private fun buildResponse(message: Message, document: Document, result: File): SendVideo = SendVideo().apply {
        chatId = message.chatId.toString()
        replyToMessageId = message.messageId
        video = loadFile(result, makeOutFilename(document.fileName, gpxAnimatorAppProperties.outputFormat), logger)
        width = gpxAnimatorAppProperties.outWidth
        height = gpxAnimatorAppProperties.outHeight
        caption = makeCaption(document)
        parseMode = "HTML"
    }

    private fun makeCaption(document: Document): String = buildString {
        append("<b>${FilenameUtils.getBaseName(document.fileName)}</b>")
        append("\n\n")
        append("From: ???").append("\n")
        append("To: ???").append("\n")
        append("Avg speed: ???").append("\n")
        append("Max speed: ???").append("\n")
        append("Uphill: ???").append("\n")
        append("Downhill: ???")
    }

    private fun makeOutFilename(sourceFilePath: String, extension: OutputFormats = OutputFormats.MP4): String {
        val sourceFileName = FilenameUtils.getBaseName(sourceFilePath)
        return "$sourceFileName.${extension.ext}"
    }

    private fun wrongResponse(response: String, userMessage: Message) {
        val message = SendMessage().apply {
            replyToMessageId = userMessage.messageId
            chatId = userMessage.chatId.toString()
            text = response
        }
        execute(message)
    }
}