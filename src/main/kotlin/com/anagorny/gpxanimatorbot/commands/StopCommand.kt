package com.anagorny.gpxanimatorbot.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class StopCommand : BotCommand("stop", "Stop using this bot") {

    override fun execute(absSender: TelegramClient, user: User, chat: Chat, arguments: Array<out String>) {
        val answer = SendMessage.builder()
            .chatId(chat.id.toString())
            .text("Good bye ${user.firstName}!\nHope to see you soon!")
            .build()
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error(e) { "Error while processing command from user='${user.userName}': " }
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
