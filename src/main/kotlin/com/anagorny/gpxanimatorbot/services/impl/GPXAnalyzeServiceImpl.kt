package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.clients.GeocoderClient
import com.anagorny.gpxanimatorbot.helpers.GeoHelper
import com.anagorny.gpxanimatorbot.helpers.average
import com.anagorny.gpxanimatorbot.helpers.middle
import com.anagorny.gpxanimatorbot.model.ElevationResult
import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import io.jenetics.jpx.*
import io.jenetics.jpx.format.Location
import io.jenetics.jpx.format.LocationFormatter
import io.jenetics.jpx.geom.Geoid
import mu.KLogging
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.asSequence
import kotlin.streams.toList

@Service
class GPXAnalyzeServiceImpl(
    private val geocoderClient: GeocoderClient,
    private val geoHelper: GeoHelper
) : GPXAnalyzeService {

    override suspend fun doAnalyze(file: File): GPXAnalyzeResult {
        logger.info { "Starting analyze of GPX file ${file.absolutePath}" }
        val gpx = readGPX(file)

        val result = GPXAnalyzeResult().apply {
            from = getCity(startPoint(gpx))
            through = getCity(throughPoint(gpx))
            to = getCity(finishPoint(gpx))
            duration = totalTime(gpx)
            distance = totalDistance(gpx)?.to(Length.Unit.KILOMETER)
            maxSpeed = maxSpeed(gpx)
            avgSpeed = avgSpeed(gpx)
            calculateElevation(gpx).let {
                ascent = it.first
                descent = it.second
            }
        }
        logger.info { "Finishing analyze of GPX file ${file.absolutePath}" }
        return result
    }


    override fun readGPX(file: File): GPX {
        val res = GPX.Reader.of(GPX.Reader.Mode.LENIENT).read(file.toPath())
        logger.info { "GPX file ${file.absolutePath} was read and ready for analyze" }
        return res
    }

    override fun startPoint(gpx: GPX): WayPoint? = gpx.tracks.first()
        .segments.first()
        .points().asSequence().first()

    override fun finishPoint(gpx: GPX): WayPoint? = gpx.tracks.last()
        .segments.last()
        .points().asSequence().last()

    override fun totalDistance(gpx: GPX): Length? = getAllPointsAsStream(gpx)
        .collect(Geoid.WGS84.toPathLength())

    override fun getAllPoints(gpx: GPX): List<WayPoint> = getAllPointsAsStream(gpx).collect(Collectors.toList())

    override fun getAllPointsAsStream(gpx: GPX): Stream<WayPoint> = gpx.tracks()
        .flatMap(Track::segments)
        .flatMap(TrackSegment::points)

    override fun totalTime(gpx: GPX): Duration {
        val points = getAllPoints(gpx)
        return Duration.between(points.first().time.get(), points.last().time.get())
    }


    private fun avgSpeed(gpx: GPX): Double? {
        val average = gpx.tracks.asSequence()
            .mapNotNull(geoHelper::avgSpeedOfTrack)
            .average()

        return if (average.isNaN()) null else average
    }

    private fun maxSpeed(gpx: GPX): Double? {
        val max = gpx.tracks.asSequence()
            .mapNotNull(geoHelper::maxSpeedOfTrack)
            .average()

        return if (max.isNaN()) null else max
    }

    private fun calculateElevation(gpx: GPX): Pair<ElevationResult, ElevationResult> {
        val points = getAllPoints(gpx)
        var pathStartPoint = 0
        val ascending = ElevationResult()
        val descending = ElevationResult()
        var isAscending = true

        for (i in 0 until points.lastIndex) {
            val j = i + 1
            val a = points[i]
            val b = points[j]
            val eleA = a.elevation.map { it.toDouble() }.orElse(0.0)
            val eleB = b.elevation.map { it.toDouble() }.orElse(0.0)

            ascending.extremum = max(max(eleA, eleB), ascending.extremum ?: 0.0)
            descending.extremum = min(min(eleA, eleB), descending.extremum ?: Double.MAX_VALUE)

            if (eleA > eleB) {
                descending.elevation = (descending.elevation ?: 0.0).plus(eleA - eleB)
                if (isAscending) {

                    var pathLength = 0.0
                    for (k in pathStartPoint until i) {
                        pathLength += Geoid.WGS84.distance(points[k], points[k + 1]).to(Length.Unit.METER)
                    }
                    ascending.totalDistance = ascending.totalDistance?.plus(pathLength)
                    ascending.maxDistance = max(pathLength, ascending.maxDistance ?: 0.0)

                    val sectionDuration = Duration.between(points[pathStartPoint].time.get(), points[i].time.get())
                    ascending.totalDuration = ascending.totalDuration?.plus(sectionDuration)
                    ascending.maxDuration = max(sectionDuration, ascending.maxDuration)

                    val partSpeeds = geoHelper.calculateSpeeds(points.subList(pathStartPoint, i)).toList()
                    if (partSpeeds.isNotEmpty()) {
                        ascending.sectionMaxSpeed = max(partSpeeds.max(), ascending.sectionMaxSpeed ?: 0.0)
                        ascending.avgSpeed = average(ascending.avgSpeed, partSpeeds.average())
                    }

//                    val elevationAngle = abs(calculateElevationAngle(points[pathStartPoint], a))
//                    ascending.maximumAngle = max(elevationAngle, ascending.maximumAngle?:0.0)

                    pathStartPoint = i
                    isAscending = false
                }
            } else {
                ascending.elevation = (ascending.elevation ?: 0.0).plus(eleB - eleA)
                if (!isAscending) {

                    var pathLength = 0.0
                    for (k in pathStartPoint until i) {
                        pathLength += Geoid.WGS84.distance(points[k], points[k + 1]).to(Length.Unit.METER)
                    }
                    descending.totalDistance = descending.totalDistance?.plus(pathLength)
                    descending.maxDistance = max(pathLength, descending.maxDistance ?: 0.0)

                    val sectionDuration = Duration.between(points[pathStartPoint].time.get(), points[i].time.get())
                    descending.totalDuration = descending.totalDuration?.plus(sectionDuration)
                    descending.maxDuration = max(sectionDuration, descending.maxDuration)


                    val partSpeeds = geoHelper.calculateSpeeds(points.subList(pathStartPoint, i)).toList()
                    if (partSpeeds.isNotEmpty()) {
                        descending.sectionMaxSpeed = max(partSpeeds.max(), descending.sectionMaxSpeed ?: 0.0)
                        descending.avgSpeed = average(descending.avgSpeed, partSpeeds.average())
                    }
//                    val elevationAngle = abs(calculateElevationAngle(points[pathStartPoint], a))
//                    descending.maximumAngle = max(elevationAngle, descending.maximumAngle?:0.0)

                    pathStartPoint = i
                    isAscending = true
                }
            }
        }
        return ascending to descending
    }


    // ToDo
//    private fun calculateElevationAngle(a: Point, b: Point): Double {
//        val dy: Double = abs(b.latitude.toRadians() - a.latitude.toRadians())
//        val dx: Double = cos(PI / 180 * a.latitude.toRadians()) * abs(b.longitude.toRadians() - a.longitude.toRadians())
//        val vert: Double = sqrt(dy*dy+dx*dx)
//        val horiz: Double = b.elevation.map { it.to(Length.Unit.METER) }.orElse(0.0) - a.elevation.map { it.to(Length.Unit.METER) }.orElse(0.0)
//        return atan(vert/horiz)
//    }

    private fun max(a: Duration?, b: Duration?): Duration {
        return if ((a ?: Duration.ZERO) >= (b ?: Duration.ZERO)) a ?: Duration.ZERO else b ?: Duration.ZERO

    }

    override fun totalAscending(gpx: GPX): Double? = null
    override fun totalDescending(gpx: GPX): Double? = null
    private fun throughPoint(gpx: GPX): WayPoint? {
        return gpx.tracks()
            .flatMap { it.segments() }
            .flatMap { it.points() }
            .middle().orElse(null)
    }

    private fun getCity(wayPoint: WayPoint?): String = try {
        val geoJson = geocoderClient.reverse(wayPoint!!.longitude.toDegrees(), wayPoint.latitude.toDegrees())

        val properties = geoJson.features.last().properties as Map<String, Any?>
        val city = properties["city"] as String?
        city ?: LocationFormatter.ISO_HUMAN_LONG.format(Location.of(wayPoint))
    } catch (e: Exception) {
        logger.error(e) { "Couldn't get city name from external geocoder" }
        null
    } ?: LocationFormatter.ISO_HUMAN_LONG.format(Location.of(wayPoint))

    companion object : KLogging()
}
