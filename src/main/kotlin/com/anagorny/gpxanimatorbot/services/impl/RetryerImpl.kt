package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.RetryerProperties
import feign.RetryableException
import feign.Retryer
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class RetryerImpl(
    private val retryerProperties: RetryerProperties
) : Retryer {
    override fun continueOrPropagate(e: RetryableException) {
        throw e
    }

    override fun clone(): Retryer {
        return Retryer.Default(
            retryerProperties.period.toMillis(),
            retryerProperties.maxPeriod.toMillis(),
            retryerProperties.maxAttempts
        )
    }

    companion object : KLogging()
}