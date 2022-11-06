package com.anagorny.gpxanimatorbot.helpers

import org.apache.commons.lang3.time.DurationFormatUtils
import java.time.Duration
import java.util.*

fun Float.format(digitsAfterPoint: Int) = "%.${digitsAfterPoint}f".format(Locale.ENGLISH, this)

fun Double.format(digitsAfterPoint: Int) = "%.${digitsAfterPoint}f".format(Locale.ENGLISH, this)

fun Duration?.format() = DurationFormatUtils.formatDuration(this?.toMillis() ?:0, "HH:mm:ss")
