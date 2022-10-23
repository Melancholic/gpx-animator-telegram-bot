package com.anagorny.gpxanimatorbot.utils

import java.io.BufferedReader

import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer


internal class StreamGobbler(
    private val inputStream: InputStream,
    private val consumeInputLine: Consumer<String?>
) : Runnable {
    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}