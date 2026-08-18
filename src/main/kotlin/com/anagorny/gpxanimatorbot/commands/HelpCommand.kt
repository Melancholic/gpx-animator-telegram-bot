package com.anagorny.gpxanimatorbot.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.generics.TelegramClient

@Component
class HelpCommand(private val context: ApplicationContext) :
    BotCommand("help", "Get all the commands this bot provides") {

    override fun execute(absSender: TelegramClient, user: User, chat: Chat, arguments: Array<String>) {
        val helpMessageBuilder = StringBuilder("Just send a *.gpx file and get a video of the route in the answer.")
            .append("\n\nRegistered commands for this bot:\n\n")
        for (botCommand in commandRegistry().registeredCommands) {
            helpMessageBuilder.append(botCommand.toString()).append("\n\n")
        }
        val helpMessage = SendMessage.builder()
            .chatId(chat.id.toString())
            .parseMode("HTML")
            .text(helpMessageBuilder.toString())
            .build()
        try {
            absSender.execute(helpMessage)
        } catch (e: TelegramApiException) {
            logger.error(e) { "Error while processing command from user='${user.userName}': " }
        }
    }

    private fun commandRegistry(): ICommandRegistry = context.getBean(ICommandRegistry::class.java)

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
