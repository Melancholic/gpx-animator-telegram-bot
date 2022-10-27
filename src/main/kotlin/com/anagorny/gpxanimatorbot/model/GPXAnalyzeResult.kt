package com.anagorny.gpxanimatorbot.model

import java.time.Duration

data class GPXAnalyzeResult(
    var from: String? = null,
    var to: String? = null,
    var duration: Duration? = null,
    var distance: Double? = null,
    var avgSpeed: Double? = null,
    var maxSpeed: Double? = null,
    var ascent: ElevationResult = ElevationResult(),
    var descent: ElevationResult = ElevationResult()
)

data class ElevationResult(
    var elevation: Double = 0.0,
    var totalDistance: Double = 0.0,
    var maxDistance: Double? = null,
    var totalDuration: Duration = Duration.ZERO,
    var maxDuration: Duration? = null,
    var maxAngle: Double? = null
)