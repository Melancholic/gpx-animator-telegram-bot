package com.anagorny.gpxanimatorbot.helpers

import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult

fun generateTripName(result: GPXAnalyzeResult, defaultTripName: String): String {
    if (allIsNull(result.from, result.to)) {
        return defaultTripName
    }
    return if (allIsEquals(result.from, result.through, result.to)) {
        "Trip around <b><i>${result.from!!}</i></b>"
    } else {
        "Trip <b><i>${result.from!!}</i></b> \u279C ${result.through?.let { "<b><i>${it}</i></b> \u279C" }} <b><i>${result.to}</i></b>"
    }
}

fun makeCaption(gpxAnalyzeResult: GPXAnalyzeResult?, defaultTripName: String): String = buildString {
    gpxAnalyzeResult?.let { result ->
        append("<b>${generateTripName(result, defaultTripName)}</b>")
        append("\n\n")
        result.from?.let { append("<b>\uD83C\uDFE1 From:</b> <i>$it</i> \n") }
        result.to?.let { append("<b>⛰️ To:</b> <i>$it</i> \n") }
        result.duration?.let { append("<b>⏰ Duration:</b> <i>${it.format()}</i>\n") }
        result.distance?.let { append("<b>\uD83D\uDCCF Distance:</b> <i>${it.format(3)}km.</i>\n") }
        result.avgSpeed?.let { append("<b>\uD83D\uDE80 Average speed:</b> <i>${it.format(2)}km/h.</i>\n") }
        result.maxSpeed?.let { append("<b>\uD83D\uDE80 Max speed:</b> <i>${it.format(2)}km/h.</i>\n") }
        append("\n")
        result.ascent.elevation?.let { append("<b>↗️ Uphill:</b> <i>${it.format(2)}m.</i>\n") }
        result.ascent.extremum?.let { append("<b>↗️ Highest point:</b> <i>${it.format(2)}m.</i>\n") }
        result.ascent.totalDistance?.let { append("<b>↗️ Total (distance):</b> <i>${it.format(2)}m.</i>\n") }
        result.ascent.maxDistance?.let { append("<b>↗️ Longest section (distance):</b> <i>${it.format(2)}m.</i>\n") }
        result.ascent.totalDuration?.let { append("<b>↗️ Total (duration):</b> <i>${it.format()}</i>\n") }
        result.ascent.maxDuration?.let { append("<b>↗️ Longest section (duration):</b> <i>${it.format()}</i>\n") }
        result.ascent.sectionMaxSpeed?.let { append("<b>↗️ Max section speed :</b> <i>${it.format(2)}km/h.</i>\n") }
        result.ascent.avgSpeed?.let { append("<b>↗️ Average section speed :</b> <i>${it.format(2)}km/h.</i>\n") }
        result.ascent.maximumAngle?.let { append("<b>↗️ Max angle:</b> <i>${it.format(2)}m.</i>\n") }
        append("\n")
        result.descent.elevation?.let { append("<b>↘️ Downhill:</b> <i>${it.format(2)}m.</i>\n") }
        result.descent.extremum?.let { append("<b>↘️ Lowest point:</b> <i>${it.format(2)}m.</i>\n") }
        result.descent.totalDistance?.let { append("<b>↘️ Total (distance):</b> <i>${it.format(2)}m.</i>\n") }
        result.descent.maxDistance?.let { append("<b>↘️ Longest section (distance):</b> <i>${it.format(2)}m.</i>\n") }
        result.descent.totalDuration?.let { append("<b>↘️ Total (duration):</b> <i>${it.format()}</i>\n") }
        result.descent.maxDuration?.let { append("<b>↘️ Longest section (duration):</b> <i>${it.format()}</i>\n") }
        result.descent.sectionMaxSpeed?.let { append("<b>↘️ Max section speed :</b> <i>${it.format(2)}km/h.</i>\n") }
        result.descent.avgSpeed?.let { append("<b>↘️ Average section speed :</b> <i>${it.format(2)}km/h.</i>\n") }
        result.descent.maximumAngle?.let { append("<b>↘️ Max angle:</b> <i>${it.format(2)}m.</i>\n") }
    }
}
