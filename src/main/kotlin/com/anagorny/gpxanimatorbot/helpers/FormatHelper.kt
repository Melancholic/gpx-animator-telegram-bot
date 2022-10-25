package com.anagorny.gpxanimatorbot.helpers

import java.util.*

fun Float.format(digitsAfterPoint: Int): String {
    return "%.${digitsAfterPoint}f".format(Locale.ENGLISH, this)
}

fun Double.format(digitsAfterPoint: Int): String {
    return "%.${digitsAfterPoint}f".format(Locale.ENGLISH, this)
}