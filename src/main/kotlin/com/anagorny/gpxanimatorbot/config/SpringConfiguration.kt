package com.anagorny.gpxanimatorbot.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.client.RestTemplate


@Configuration
open class SpringConfiguration {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        // Do any additional configuration here
        return builder.build()
    }

    @Bean
    fun threadPoolTaskExecutor(properties: ConcurrencyProperties): AsyncListenableTaskExecutor {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = properties.coreSize
        threadPoolTaskExecutor.maxPoolSize = properties.maxSize
        return threadPoolTaskExecutor
    }

    @Bean
    fun jsonMapper(): ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder()
                .build()
        )


//    @Bean
//    fun ffmpeg(properties: MediaProperties) = FFmpeg(properties.ffmpeg.path)
//
//    @Bean
//    fun ffprobe(properties: MediaProperties) = FFprobe(properties.ffprobe.path)
//
//    @Bean
//    fun ffmpegTaskExecutor(ffmpeg: FFmpeg, ffprobe: FFprobe) = FFmpegExecutor(ffmpeg, ffprobe)


}