package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.ForecastProperties
import com.anagorny.gpxanimatorbot.helpers.launchAsync
import com.anagorny.gpxanimatorbot.helpers.measureTime
import com.anagorny.gpxanimatorbot.services.ForecastService
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import com.anagorny.gpxanimatorbot.services.GpxProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct
import kotlin.math.roundToLong

@Service("forecastService")
@ConditionalOnProperty("forecast.enabled")
class ForecastServiceImpl(
    private val properties: ForecastProperties,
    private val gpxAnalyzeService: GPXAnalyzeService,
    private val gpxProcessor: GpxProcessor,
    @Qualifier("mainFlowCoroutineScope")
    private val scope: CoroutineScope
) : ForecastService {
    private var isInitialized = false

    private var refPointsCount: Long? = null
    private lateinit var refDuration: Duration

    @PostConstruct
    fun postConstruct() {
        logger.info { "Forecast enabled, run initializing" }
        scope.launchAsync {
            MDC.put("correlationId", "FORECAST")
            initialize()
        }.invokeOnCompletion { MDC.clear() }
    }

    override fun isInitialized(): Boolean = isInitialized

    override suspend fun initialize() {
        val refGpxFile = File(properties.testGpxPath)
        val refGpx = gpxAnalyzeService.readGPX(refGpxFile)
        refDuration = measureTime {
            runBlocking {
                gpxProcessor.doProcess(refGpxFile)
            }
        }.first

        refPointsCount = gpxAnalyzeService.getAllPointsAsStream(refGpx).count()
        isInitialized = true
        logger.info { "Forecast initialized successful" }
    }

    override suspend fun forecast(gpxFile: File): Optional<Duration> {
        return if (isInitialized()) {
            val gpx = gpxAnalyzeService.readGPX(gpxFile)
            val pointsCount = gpxAnalyzeService.getAllPointsAsStream(gpx).count()
            val forecastDuration =
                Duration.ofMillis(((pointsCount / refPointsCount!! * 1.0) * refDuration.toMillis()).roundToLong())
            Optional.of(forecastDuration)
        } else {
            Optional.empty()
        }
    }

    companion object : KLogging()
}


