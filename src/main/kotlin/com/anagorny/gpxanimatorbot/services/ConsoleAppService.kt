package com.anagorny.gpxanimatorbot.services

import mu.KLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service


@Service
class ConsoleAppService : CommandLineRunner {

    override fun run(vararg args: String?) {
        logger.info("Application was running with args: ${args.contentToString()}")
    }

    companion object : KLogging()
}
