package com.anagorny.gpxanimatorbot.services

import mu.KLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service


@Service
class ConsoleAppService(
    val gpxAnimatorRunner: GpxAnimatorRunner
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        logger.info("Console app started with args: ${args.contentToString()}")
        gpxAnimatorRunner.runTest()
        logger.info("Application was running")
    }

    companion object : KLogging()
}