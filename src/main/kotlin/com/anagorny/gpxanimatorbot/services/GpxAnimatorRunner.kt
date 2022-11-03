package com.anagorny.gpxanimatorbot.services

import java.io.File

interface GpxAnimatorRunner {
    fun runTest()
    fun run(inFilePath: String, outFilePath: String): File
}
