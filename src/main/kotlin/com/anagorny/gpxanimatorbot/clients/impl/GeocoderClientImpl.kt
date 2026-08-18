package com.anagorny.gpxanimatorbot.clients.impl

import com.anagorny.gpxanimatorbot.clients.GeocoderClient
import com.anagorny.gpxanimatorbot.clients.ReverseGeocodingResult
import com.anagorny.gpxanimatorbot.config.GeocoderProperties
import com.anagorny.gpxanimatorbot.config.RetryerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import kotlin.math.min

@Service
class GeocoderClientImpl(
    geocoderProperties: GeocoderProperties,
    private val retryerProperties: RetryerProperties,
    @Qualifier("geocoderRestTemplateBuilder")
    restTemplateBuilder: RestTemplateBuilder
) : GeocoderClient {
    private val restTemplate = restTemplateBuilder.build()
    private val reverseUrl = "${geocoderProperties.url.trimEnd('/')}/reverse"

    override fun reverse(lon: Double, lat: Double, lang: String): ReverseGeocodingResult {
        val url = UriComponentsBuilder.fromUriString(reverseUrl)
            .queryParam("lon", lon)
            .queryParam("lat", lat)
            .queryParam("lang", lang)
            .toUriString()

        return withRetry {
            logger.debug { "Requesting reverse geocoding: $url" }
            restTemplate.getForObject(url, ReverseGeocodingResult::class.java)
                ?: ReverseGeocodingResult()
        }
    }

    // Mirrors what feign's Retryer.Default used to do for this client: retry up to
    // `maxAttempts` with a 1.5x backoff capped at `maxPeriod`.
    private fun <T> withRetry(block: () -> T): T {
        var attempt = 1
        var interval = retryerProperties.period.toMillis()
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                if (attempt >= retryerProperties.maxAttempts) throw e
                logger.warn { "Geocoder call failed (attempt $attempt/${retryerProperties.maxAttempts}): ${e.message}" }
                Thread.sleep(interval)
                interval = min((interval * 1.5).toLong(), retryerProperties.maxPeriod.toMillis())
                attempt++
            }
        }
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
