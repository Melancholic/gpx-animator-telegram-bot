package com.anagorny.gpxanimatorbot.handlers

import mu.KLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class MainHandler(
    private val fileMessageHandler: UpdatesHandler,
    private val otherMessageHandler: UpdatesHandler
) : UpdatesHandler {

    override suspend fun handle(update: Update) {
        if (update.hasMessage()) {
            if (update.message.hasDocument()) {
                fileMessageHandler.handle(update)
            } else {
                otherMessageHandler.handle(update)
            }
        }

    }

    companion object : KLogging()
}