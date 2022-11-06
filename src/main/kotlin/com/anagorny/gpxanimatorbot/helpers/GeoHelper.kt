@file:OptIn(ExperimentalStdlibApi::class)

package com.anagorny.gpxanimatorbot.helpers

import io.jenetics.jpx.*
import io.jenetics.jpx.geom.Geoid
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import kotlin.jvm.optionals.getOrNull

@Service
class GeoHelper {

    fun avgSpeedOfTrack(track: Track) = track.segments()
        .mapToDouble { avgSpeedOfSegment(it).orElse(Double.NaN) }
        .filter {it > 0}
        .average().orElse(Double.NaN)

    fun maxSpeedOfTrack(track: Track) = track.segments()
        .mapToDouble { maxSpeedOfSegment(it).orElse(Double.NaN) }
        .filter {it > 0}
        .max().orElse(Double.NaN)

    fun avgSpeedOfSegment(segment: TrackSegment): OptionalDouble {
        val speedsFromGpx = segment.points.mapNotNull { it.speed.getOrNull() }.toList()
        val result = if (speedsFromGpx.isEmpty()) {
            calculateSpeeds(segment.points().collect(Collectors.toList())).average()
        } else {
            speedsFromGpx.stream().mapToDouble { it.to(Speed.Unit.KILOMETERS_PER_HOUR) }.average()
        }
        return result
    }

    fun maxSpeedOfSegment(segment: TrackSegment): OptionalDouble {
        val speedsFromGpx = segment.points.mapNotNull { it.speed.getOrNull() }.toList()
        val result = if (speedsFromGpx.isEmpty()) {
            calculateSpeeds(segment.points().collect(Collectors.toList())).max()
        } else {
            speedsFromGpx.stream().mapToDouble { it.to(Speed.Unit.KILOMETERS_PER_HOUR) }.max()
        }
        return result
    }


    fun calculateSpeeds(points: List<WayPoint>): DoubleStream {
        if (points.isEmpty()) return DoubleStream.empty()
        val firstPointTime = points.first().time.get()

        return points.asSequence()
            .filter { it.time.isPresent }
            .groupBy {Duration.between(firstPointTime, it.time.get()).toMinutes()+1 }
            .map { (minute, points) ->
                var distance = 0.0
                for (i in 0 until points.size - 1) {
                    distance += Geoid.WGS84.distance(points[i], points[i + 1]).to(Length.Unit.METER)
                }
                return@map minute to distance
            }.stream()
            .mapToDouble {(_, distance) -> (distance/1000.0) / (1/60.0)}
    }
}
