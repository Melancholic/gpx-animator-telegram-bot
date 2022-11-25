package com.anagorny.gpxanimatorbot.config

import com.anagorny.gpxanimatorbot.helpers.coroutineScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration

@Configuration
//ToDo remove it after migration telegrambots-spring-boot-starter to spring boot 3.0
@Import(value = [TelegramBotStarterConfiguration::class])
class SpringConfiguration(
    val properties: SystemProperties
) {

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
    fun jsonMapper(): ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder()
                .build()
        )
}
