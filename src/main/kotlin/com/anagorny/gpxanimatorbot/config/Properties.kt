package com.anagorny.gpxanimatorbot.config

import com.anagorny.gpxanimatorbot.model.OutputFormats
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.time.Duration

@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val chatId: String, val bot: BotProperties
) {
    data class BotProperties(
        val token: String, val name: String
    )
}

@ConfigurationProperties(prefix = "system")
data class SystemProperties(
    val workDir: String,
    val inputFileMaxSize: DataSize,
    val rateLimiting: RateLimiterProperties = RateLimiterProperties(),
    val executor: ExecutorProperties = ExecutorProperties()
) {
    data class ExecutorProperties(val coreSize: Int = 5, val maxSize: Int = 10)
}

@ConfigurationProperties(prefix = "gpx-animator-app")
data class GpxAnimatorAppProperties(
    val path: String,
    val executionTimeout: Duration,
    val outputFormat: OutputFormats = OutputFormats.MP4,
    val outWidth: Int,
    val outHeight: Int,
    val backgroundMapVisibility: Float,
    val fps: Int
)

@ConfigurationProperties(prefix = "retryer")
data class RetryerProperties(
    val maxAttempts: Int,
    val period: Duration,
    val maxPeriod: Duration,
)

data class RateLimiterProperties(
    val enabled: Boolean = true, val limits: Set<LimitProperties> = emptySet()
) {
    data class LimitProperties(val requests: Long, val period: Duration)
}

@ConfigurationProperties(prefix = "forecast")
data class ForecastProperties(
    val enabled: Boolean, val testGpxPath: String
)
