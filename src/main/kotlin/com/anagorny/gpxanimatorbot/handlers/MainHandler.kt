package com.anagorny.gpxanimatorbot.handlers

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class MainHandler(
    private val fileMessageHandler: UpdatesHandler,
    private val otherMessageHandler: UpdatesHandler
) : UpdatesHandler {

    override suspend fun handle(update: Update) = withContext(MDCContext()) {
        if (update.hasMessage()) {
            val message = update.message
            logger.info { "Got message with id=${message.messageId} in chat ${message.chatId} from ${message.from.userName}(${message.from.id})" }
            if (message.hasDocument()) {
                fileMessageHandler.handle(update)
            } else {
                otherMessageHandler.handle(update)
            }
        }
    }

    companion object : KLogging()
}
