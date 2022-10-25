package com.anagorny.gpxanimatorbot.model

import java.time.Duration

data class GPXAnalyzeResult(
    var from: String? = null,
    var to: String? = null,
    var duration: Duration? = null,
    var distance: Double? = null,
    var avgSpeed: Double? = null,
    var maxSpeed: Double? = null,
    var uphill: Double? = null,
    var downhill: Double? = null
)