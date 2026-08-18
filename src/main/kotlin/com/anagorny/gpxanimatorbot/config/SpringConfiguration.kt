package com.anagorny.gpxanimatorbot.config

import com.anagorny.gpxanimatorbot.helpers.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient

@Configuration
class SpringConfiguration(
    val properties: SystemProperties
) {

    @Bean
    fun telegramClient(telegramProperties: TelegramProperties): TelegramClient =
        OkHttpTelegramClient(telegramProperties.bot.token)

    @Bean
    fun geocoderRestTemplateBuilder(geocoderProperties: GeocoderProperties): RestTemplateBuilder =
        RestTemplateBuilder()
            .connectTimeout(geocoderProperties.connectTimeout)
            .readTimeout(geocoderProperties.readTimeout)

    @Bean
    fun threadPoolTaskExecutor(): AsyncTaskExecutor {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = properties.executor.coreSize
        threadPoolTaskExecutor.maxPoolSize = properties.executor.maxSize
        return threadPoolTaskExecutor
    }

    @Bean
    fun mainFlowCoroutineScope(): CoroutineScope = coroutineScope(
        properties.executor.coreSize,
        properties.executor.maxSize
    ) + MDCContext()

    @Bean
    fun loggingProcessCoroutineScope(): CoroutineScope = coroutineScope(3) + MDCContext() + SupervisorJob()
}
