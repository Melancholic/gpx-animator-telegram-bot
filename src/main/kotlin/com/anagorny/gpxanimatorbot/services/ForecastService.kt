package com.anagorny.gpxanimatorbot.services

import java.io.File
import java.time.Duration
import java.util.*

interface ForecastService {
    fun isInitialized() : Boolean
    suspend fun initialize()
    suspend fun forecast(gpxFile: File): Optional<Duration>
}
