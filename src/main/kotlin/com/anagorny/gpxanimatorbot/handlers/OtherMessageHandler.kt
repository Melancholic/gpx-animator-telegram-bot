package com.anagorny.gpxanimatorbot.handlers

import com.anagorny.gpxanimatorbot.services.MainTelegramBotService
import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class OtherMessageHandler(
    private val botService: MainTelegramBotService
) : UpdatesHandler {
    override suspend fun handle(update: Update) {
        val message = update.message
        logger.info("Unknown message id=${message.messageId}, text: '${message.text}'")
        botService.execute(DeleteMessage().apply {
            chatId = message.chatId.toString()
            messageId = message.messageId
        })
        logger.info("Message with id=${message.messageId} was deleted")
    }

    companion object : KLogging()
}