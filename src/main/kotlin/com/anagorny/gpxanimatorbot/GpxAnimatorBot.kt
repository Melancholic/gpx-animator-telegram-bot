package com.anagorny.gpxanimatorbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableScheduling
@EnableRetry
@EnableAsync
class GpxAnimatorBot

fun main(args: Array<String>) {
	runApplication<GpxAnimatorBot>(*args)
}

