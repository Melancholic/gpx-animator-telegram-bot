package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.SystemProperties
import com.anagorny.gpxanimatorbot.services.RateLimiter
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


@Service
class RateLimiterImpl(
    private val systemProperties: SystemProperties
) : RateLimiter {
    private val NUM_TOKENS = 1L
    private val cache: MutableMap<String, Bucket> = ConcurrentHashMap()

    override fun isRateLimitingEnabled(): Boolean = systemProperties.rateLimiting.enabled

    override fun isRequestAllowed(apiKey: String): Boolean {
        return if (isRateLimitingEnabled()) {
            val bucket: Bucket = resolveBucket(apiKey)
            val probe = bucket.tryConsumeAndReturnRemaining(NUM_TOKENS)
            probe.isConsumed
        } else {
            true
        }
    }

    override fun howLongForAllow(apiKey: String): Duration {
        return if (isRateLimitingEnabled()) {
            val bucket = resolveBucket(apiKey)
            val probe = bucket.estimateAbilityToConsume(NUM_TOKENS)
            Duration.ofNanos(probe.nanosToWaitForRefill)
        } else {
            return Duration.ofNanos(0)
        }

    }

    private fun resolveBucket(apiKey: String): Bucket {
        return cache.computeIfAbsent(apiKey) { getBucket() }
    }

    private fun getBucket(): Bucket {
        val rateLimiterProps = systemProperties.rateLimiting
        val builder = Bucket.builder()
        rateLimiterProps.limits.forEach {
            builder.addLimit(Bandwidth.classic(it.requests, Refill.intervally(it.requests, it.duration)))
        }
        return builder.build()
    }
}