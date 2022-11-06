package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.services.ForecastService
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.*

@Service
@ConditionalOnMissingBean(name = ["forecastService"])
class ForecastStubServiceImpl : ForecastService {
    override fun isInitialized(): Boolean = false

    override suspend fun initialize() { }

    override suspend fun forecast(gpxFile: File): Optional<Duration> = Optional.empty()

    companion object : KLogging()
}
