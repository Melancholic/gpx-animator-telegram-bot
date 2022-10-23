//package com.anagorny.gpxanimatorbot.services
//
//import com.anagorny.gpxanimatorbot.config.TelegramProperties
//import org.slf4j.LoggerFactory
//import org.springframework.stereotype.Component
//import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
//import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage
//import org.telegram.telegrambots.meta.api.objects.Update
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException
//
//
//@Component
//class BotCommandHandler(
//    private val telegramProperties: TelegramProperties,
//    commands: Set<IBotCommand>
//    ) : TelegramLongPollingCommandBot() {
//    private val logger = LoggerFactory.getLogger(BotCommandHandler::class.java)
//
//    override fun getBotUsername() = telegramProperties.bot.name
//
//    override fun getBotToken() = telegramProperties.bot.token
//
//    init {
//        registerAll(*commands.toTypedArray())
//    }
//
//    override fun processNonCommandUpdate(update: Update) {
//        if (update.hasMessage()) {
//            val message = update.message
//            val user = message.from
//
//            if (message.hasText()) {
//                val echoMessage = SendMessage()
//                echoMessage.setChatId(message.chatId)
//                echoMessage.text = """
//                Unknown command:
//                ${message.text}
//                Try reading /help.
//                """
//                try {
//                    execute(echoMessage)
//                } catch (e: TelegramApiException) {
//                    logger.error("Error while processing message from user='{}': ", user.id, e)
//                }
//            }
//        }
//    }
//}