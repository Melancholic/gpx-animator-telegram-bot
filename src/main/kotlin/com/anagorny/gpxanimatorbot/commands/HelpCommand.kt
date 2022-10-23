package com.anagorny.gpxanimatorbot.commands

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class HelpCommand(private val context: ApplicationContext) :
    BotCommand("help", "Get all the commands this bot provides") {
    private val logger = LoggerFactory.getLogger(HelpCommand::class.java)

    override fun execute(absSender: AbsSender, user: User, chat: Chat, arguments: Array<String>) {
        val helpMessageBuilder = StringBuilder("Registered commands for this Bot:\n\n")
        for (botCommand in commandRegistry().registeredCommands) {
            helpMessageBuilder.append(botCommand.toString()).append("\n\n")
        }
        val helpMessage = SendMessage()
        helpMessage.chatId = chat.id.toString()
        helpMessage.enableHtml(true)
        helpMessage.text = helpMessageBuilder.toString()
        try {
            absSender.execute(helpMessage)
        } catch (e: TelegramApiException) {
            logger.error("Error while processing command from user='{}': ", user.userName, e)
        }
    }

    private fun commandRegistry(): ICommandRegistry = context.getBean(ICommandRegistry::class.java)
}