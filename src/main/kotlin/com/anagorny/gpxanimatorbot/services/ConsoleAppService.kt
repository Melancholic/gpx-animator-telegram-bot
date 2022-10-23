package com.anagorny.gpxanimatorbot.services

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service


@Service
class ConsoleAppService(
    val gpxAnimatorRunner: GpxAnimatorRunner
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(CommandLineRunner::class.java)

    override fun run(vararg args: String?) {
        logger.info("Console app started with args: ${args.contentToString()}")
        gpxAnimatorRunner.runTest()
        logger.info("Application was running")
    }

}