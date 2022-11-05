package com.anagorny.gpxanimatorbot.services

import java.io.File

interface GpxAnimatorRunner {
    fun runTest()
    suspend fun run(inFilePath: String, outFilePath: String): File
}
