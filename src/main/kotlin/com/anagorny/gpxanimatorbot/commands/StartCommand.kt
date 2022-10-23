package com.anagorny.gpxanimatorbot.commands

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class StartCommand : BotCommand("start", "Welcome command") {
    private val logger = LoggerFactory.getLogger(StartCommand::class.java)

    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<out String>) {
        val messageBuilder = StringBuilder()
        messageBuilder.append("Welcome ${user.firstName}!\n")
        messageBuilder.append("this bot will make cool videos from your GPX.\n")
        messageBuilder.append("To learn how to use it, try /help")

        val answer = SendMessage()
        answer.chatId = chat.id.toString()
        answer.text = messageBuilder.toString()
        try {
            absSender.execute(answer)
        } catch (e: TelegramApiException) {
            logger.error("Error while processing command from user='{}': ", user.userName, e)
        }
    }
}