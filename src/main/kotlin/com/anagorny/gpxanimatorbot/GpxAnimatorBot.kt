package com.anagorny.gpxanimatorbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients


@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableFeignClients
class GpxAnimatorBot

fun main(args: Array<String>) {
	runApplication<GpxAnimatorBot>(*args)
}

