package com.anagorny.gpxanimatorbot.services

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service


@Service
class ConsoleAppService : CommandLineRunner {

    override fun run(vararg args: String) {
        logger.info { "Application was running with args: ${args.contentToString()}" }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
