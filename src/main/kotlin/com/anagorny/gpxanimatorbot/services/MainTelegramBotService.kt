package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.config.TelegramProperties
import com.anagorny.gpxanimatorbot.handlers.MainHandler
import com.anagorny.gpxanimatorbot.helpers.launchAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.telegram.telegrambots.extensions.bots.commandbot.CommandLongPollingTelegramBot
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient


@Service
class MainTelegramBotService(
    private val telegramProperties: TelegramProperties,
    commands: Set<IBotCommand>,
    telegramClient: TelegramClient,
    @Qualifier("mainFlowCoroutineScope")
    private val scope: CoroutineScope
) : CommandLongPollingTelegramBot(
    telegramClient,
    true,
    { telegramProperties.bot.name }
), SpringLongPollingBot {

    @set:Autowired
    @set:Lazy
    lateinit var mainHandler: MainHandler

    init {
        registerAll(*commands.toTypedArray())
    }

    @PostConstruct
    protected fun postConstruct() {
        logger.info { "${this.javaClass.canonicalName} was initialized" }
    }

    override fun getBotToken(): String = telegramProperties.bot.token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = this

    // telegrambots 10.2.0: isCommand() reads MessageEntity.text, which is only populated as
    // a side effect of calling getEntities() - warm it before consume() checks isCommand().
    override fun consume(updates: List<Update>) {
        updates.forEach { it.message?.entities }
        super.consume(updates)
    }

    // consume() routes every update that isn't a command here, including the ones carrying
    // no message at all (edited_message, my_chat_member, message_reaction, ...) - those have
    // nothing to handle and no id to correlate on.
    override fun processNonCommandUpdate(update: Update) {
        val message = update.message ?: run {
            logger.debug { "Ignoring update ${update.updateId} that carries no message" }
            return
        }
        scope.launchAsync {
            val job = launch {
                MDC.put("correlationId", "${message.chatId}-${message.messageId}")
                mainHandler.handle(update)
            }
            job.invokeOnCompletion { MDC.clear() }
            job.join()
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
