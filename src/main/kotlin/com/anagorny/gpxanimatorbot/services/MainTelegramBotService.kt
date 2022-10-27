package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.handlers.MainHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.objects.Update
import javax.annotation.PostConstruct


@Service
class MainTelegramBotService(
    private val telegramProperties: TelegramProperties,
    commands: Set<IBotCommand>
) : TelegramLongPollingCommandBot() {

    @set:Autowired
    @set:Lazy
    lateinit var mainHandler: MainHandler

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    init {
        registerAll(*commands.toTypedArray())
    }

    @PostConstruct
    protected fun postConstruct() {
        logger.info("${this.javaClass.canonicalName} was initialized")
    }

    override fun getBotUsername() = telegramProperties.bot.name
    override fun getBotToken() = telegramProperties.bot.token


    override fun processNonCommandUpdate(update: Update) {
        scope.async(CoroutineExceptionHandler { _, exception ->
            logger.error(exception) { "CoroutineExceptionHandler got $exception" }
        }) {
            mainHandler.handle(update)
        }
    }

    fun sentAction(chatId: Long, action: ActionType) {
        execute(
            SendChatAction.builder()
                .chatId(chatId)
                .action(action.toString())
                .build()
        )
    }

    companion object : KLogging()
}