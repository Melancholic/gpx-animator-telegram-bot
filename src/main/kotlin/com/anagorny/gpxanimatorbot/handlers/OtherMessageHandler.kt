package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.helpers.deleteMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class OtherMessageHandler(
    private val telegramClient: TelegramClient
) : UpdatesHandler {
    override suspend fun handle(update: Update) {
        val message = update.message
        logger.info { "Unknown message id=${message.messageId}, text: '${message.text}'" }
        telegramClient.deleteMessage(message)
        logger.info { "Message with id=${message.messageId} was deleted" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
