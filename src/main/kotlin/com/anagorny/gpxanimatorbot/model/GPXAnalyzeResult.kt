package com.anagorny.gpxanimatorbot.model

import java.time.Duration

data class GPXAnalyzeResult(
    var from: String? = null,
    var through: String? = null,
    var to: String? = null,
    var duration: Duration? = null,
    var distance: Double? = null,
    var avgSpeed: Double? = null,
    var maxSpeed: Double? = null,
    var ascent: ElevationResult = ElevationResult(),
    var descent: ElevationResult = ElevationResult()
)

data class ElevationResult(
    var elevation: Double? = null,
    var extremum: Double? = null,
    var totalDistance: Double? = null,
    var maxDistance: Double? = null,
    var totalDuration: Duration? = null,
    var maxDuration: Duration? = null,
    var sectionMaxSpeed: Double? = null,
    var sectionMinSpeed: Double? = null,
    var maximumAngle: Double? = null
)