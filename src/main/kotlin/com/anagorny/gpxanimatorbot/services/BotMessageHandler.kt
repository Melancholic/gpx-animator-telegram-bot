package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.helpers.removeFileIfExist
import com.anagorny.gpxanimatorbot.model.OutputFormats
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File


@Component
class BotMessageHandler(
    private val telegramProperties: TelegramProperties,
    private val gpxAnimatorRunner: GpxAnimatorRunner,
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties
) : TelegramLongPollingBot() {
    private val logger = LoggerFactory.getLogger(BotMessageHandler::class.java)

    override fun getBotUsername() = telegramProperties.bot.name
    override fun getBotToken() = telegramProperties.bot.token

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage()) {
            val message = update.message

            if (message.hasDocument()) {
                val document = message.document
                if (!document.fileName.endsWith(".gpx")) {
                    wrongResponse("Your file isn't GPX (attachment must have '.gpx' extension)", message)
                    return
                }
                if (document.fileSize > DataSize.ofMegabytes(10).toBytes()) {
                    wrongResponse("Your file is larger than 10B. Files larger than 10MB are not supported.", message)
                    return
                }

                var file: File? = null
                var result: File? = null

                try {
                    file = downloadFile(execute(GetFile(document.fileId)), File("/tmp/${document.fileName}"))
                    result = gpxAnimatorRunner.run(file.absolutePath, buildOutFilePath(file.absolutePath))

                    val response = SendVideo().apply {
                        chatId = message.chatId.toString()
                        video = loadFile(result)
                        width = gpxAnimatorAppProperties.outWidth
                        height = gpxAnimatorAppProperties.outHeight
                    }
                    execute(response)
                } finally {
                    removeFileIfExist(file?.absolutePath, logger)
                    removeFileIfExist(result?.absolutePath, logger)
                }
            }
        }
    }

    private fun buildOutFilePath(sourceFilePath: String, extension: OutputFormats = OutputFormats.MP4): String {
        val sourcePath = FilenameUtils.getPath(sourceFilePath)
        val sourceFileName = FilenameUtils.getBaseName(sourceFilePath)
        return "$sourcePath$sourceFileName.${extension.ext}"
    }

    private fun wrongResponse(response: String, userMessage: Message) {
        val message = SendMessage()
        message.chatId = userMessage.chatId.toString()
        message.text = response
        execute(message)
    }

    private fun loadFile(file: File): InputFile {
        return try {
            InputFile(file)
        } catch (e: Exception) {
            logger.error("Cant read file '${file.absolutePath}' fo sending", e)
            throw e
        }
    }
}