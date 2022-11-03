package com.anagorny.gpxanimatorbot.config

import com.anagorny.gpxanimatorbot.helpers.coroutineScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate


@Configuration
class SpringConfiguration(
    val properties: SystemProperties
) {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        // Do any additional configuration here
        return builder.build()
    }

    @Bean
    fun threadPoolTaskExecutor(): AsyncListenableTaskExecutor {
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
    fun loggingProcessCoroutineScope(): CoroutineScope = coroutineScope(3) + MDCContext()

    @Bean
    fun jsonMapper(): ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder()
                .build()
        )
}
