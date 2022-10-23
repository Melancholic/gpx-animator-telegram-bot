package com.anagorny.gpxanimatorbot.config

import com.anagorny.gpxanimatorbot.model.OutputFormats
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val chatId: String,
    val bot: BotProperties,
    val sending: SendingProperties,
    val files: FilesProperties
) {
    data class BotProperties(
        val token: String,
        val name: String
    )

    data class SendingProperties(
        val ignoreError: Boolean,
        val retry: RetryConfiguration
    )

    data class FilesProperties(
        @DataSizeUnit(DataUnit.MEGABYTES)
        val audioMaxSize: DataSize
    )
}

@ConstructorBinding
@ConfigurationProperties(prefix = "system")
data class SystemProperties(
    val workDir: String
)

@ConstructorBinding
@ConfigurationProperties(prefix = "media")
data class MediaProperties(
    val workDir: String,
    val ffmpeg: ProgramProperties,
    val ffprobe: ProgramProperties,
) {
    data class ProgramProperties(
        val path: String
    )
}

@ConstructorBinding
@ConfigurationProperties(prefix = "gpx-animator-app")
data class GpxAnimatorAppProperties(
    val path: String,
    val outputFormat: OutputFormats = OutputFormats.MP4,
    val outWidth: Int,
    val outHeight: Int,
    val backgroundMapVisibility: Float,
    val fps: Int
)


@ConstructorBinding
@ConfigurationProperties(prefix = "parallel")
data class ConcurrencyProperties(
    val coreSize: Int,
    val maxSize: Int
)

data class RetryConfiguration(
    val maxAttempts: Int,
    val delay: Duration
)