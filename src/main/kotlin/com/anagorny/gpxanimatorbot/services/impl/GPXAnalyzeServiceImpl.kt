package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import com.anagorny.gpxanimatorbot.services.GeocoderClient
import io.jenetics.jpx.*
import io.jenetics.jpx.format.Location
import io.jenetics.jpx.format.LocationFormatter
import io.jenetics.jpx.geom.Geoid
import mu.KLogging
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration
import java.util.stream.DoubleStream
import java.util.stream.Stream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.streams.asSequence

@Service
class GPXAnalyzeServiceImpl(
    private val geocoderClient: GeocoderClient
) : GPXAnalyzeService {

    override fun doAnalyze(file: File): GPXAnalyzeResult {
        logger.info { "Starting analyze of GPX file ${file.absolutePath}" }
        val gpx = readGPX(file)

        val result = GPXAnalyzeResult().apply {
            from = getCity(startPoint(gpx))
            to = getCity(finishPoint(gpx))
            duration = totalTime(gpx)
            distance = totalDistance(gpx)?.to(Length.Unit.KILOMETER)
            maxSpeed = maxSpeed(gpx)
            avgSpeed = avgSpeed(gpx)
            uphill = totalAscending(gpx)
            downhill = totalDescending(gpx)
        }
        logger.info { "Finishing analyze of GPX file ${file.absolutePath}" }
        return result
    }


    override fun readGPX(file: File): GPX {
        val res = GPX.read(file.toPath())
        logger.info { "GPX file ${file.absolutePath} was read and ready for analyze" }
        return res
    }

    override fun startPoint(gpx: GPX): WayPoint? = getAllPointsAsStream(gpx)
        .asSequence()
        .last()

    override fun finishPoint(gpx: GPX): WayPoint? = getAllPointsAsStream(gpx)
        .asSequence()
        .last()

    override fun totalDistance(gpx: GPX): Length? = getAllPointsAsStream(gpx)
        .collect(Geoid.WGS84.toPathLength())

    override fun getAllPoints(gpx: GPX): List<WayPoint> = getAllPointsAsStream(gpx).toList()

    override fun getAllPointsAsStream(gpx: GPX): Stream<WayPoint> = gpx.tracks()
        .flatMap(Track::segments)
        .flatMap(TrackSegment::points)

    override fun totalTime(gpx: GPX): Duration {
        val points = getAllPoints(gpx)
        return Duration.between(points.first().time.get(), points.last().time.get())
    }


    private fun avgSpeed(gpx: GPX): Double {
        val speedsFromGpx = extractedSpeedsForPoints(gpx)
        val averageSpeed = if (speedsFromGpx.isEmpty()) {
            calculatedSpeedsForPoints(gpx).average()
        } else {
            speedsFromGpx.stream()
                .mapToDouble { it }
                .average()
        }

        return averageSpeed.orElse(0.0)
    }

    private fun maxSpeed(gpx: GPX): Double {
        val speedsFromGpx = extractedSpeedsForPoints(gpx)
        val maxSpeed = if (speedsFromGpx.isEmpty()) {
            calculatedSpeedsForPoints(gpx).max()
        } else {
            speedsFromGpx.stream()
                .mapToDouble { it }
                .max()
        }

        return maxSpeed.orElse(0.0)
    }

    private fun calculatedSpeedsForPoints(gpx: GPX): DoubleStream {
        val speeds = DoubleStream.builder()
        val points = getAllPoints(gpx)
        val totalTime = totalTime(gpx)
        val step: Int = min(10.0, totalTime.toSeconds() / points.size * 1.0).roundToInt()

        for (i in points.indices step step) {
            val j = min(i + step, points.size - 1)
            val a = points[i]
            val b = points[j]

            var distance = 0.0
            for (k in i until j) {
                distance += Geoid.WGS84.distance(points[k], points[k + 1]).to(Length.Unit.METER)
            }

            speeds.add(
                distance / ((Duration.between(
                    a.time.get(),
                    b.time.get()
                )).abs().toMillis() / 1000.0) * 3.6
            )
        }
        return speeds.build()
    }

    private fun extractedSpeedsForPoints(gpx: GPX): List<Double> {
        val points = getAllPoints(gpx)

        return points.asSequence()
            .mapNotNull { it.speed.orElse(null) }
            .map { it.to(Speed.Unit.KILOMETERS_PER_HOUR) }//ToDo parametrize i
            .toList()
    }

    override fun totalAscending(gpx: GPX): Double? = null
    override fun totalDescending(gpx: GPX): Double? = null

    private fun getCity(wayPoint: WayPoint?): String? {
        val geoJson = geocoderClient.reverse(wayPoint!!.longitude.toDegrees(), wayPoint.latitude.toDegrees())
        val properties = geoJson.features.last().properties as Map<String, Any?>
        val city = properties["city"] as String?
        return city ?: LocationFormatter.ISO_HUMAN_LONG.format(Location.of(wayPoint))
    }

    companion object : KLogging()
}