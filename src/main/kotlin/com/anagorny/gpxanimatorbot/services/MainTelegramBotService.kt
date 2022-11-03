package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.handlers.MainHandler
import com.anagorny.gpxanimatorbot.helpers.launchAsync
import kotlinx.coroutines.CoroutineScope
import mu.KLogging
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    commands: Set<IBotCommand>,
    @Qualifier("mainFlowCoroutineScope")
    private val scope: CoroutineScope
) : TelegramLongPollingCommandBot() {

    @set:Autowired
    @set:Lazy
    lateinit var mainHandler: MainHandler

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
        scope.launchAsync {
            MDC.put("correlationId", "${update.message.chatId}-${update.message.messageId}")
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
