@file:OptIn(ExperimentalStdlibApi::class)

package com.anagorny.gpxanimatorbot.helpers

import io.jenetics.jpx.*
import io.jenetics.jpx.geom.Geoid
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

@Service
class GeoHelper {

    fun avgSpeedOfTrack(track: Track) = track.segments()
        .mapToDouble { avgSpeedOfSegment(it).orElse(Double.NaN) }
        .average().orElse(Double.NaN)

    fun maxSpeedOfTrack(track: Track) = track.segments()
        .mapToDouble { maxSpeedOfSegment(it).orElse(Double.NaN) }
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
            // ToDo calculate max speed after track filtering
            OptionalDouble.empty()
        } else {
            speedsFromGpx.stream().mapToDouble { it.to(Speed.Unit.KILOMETERS_PER_HOUR) }.max()
        }
        return result
    }


    fun calculateSpeeds(points: List<WayPoint>): DoubleStream {
        val speeds = DoubleStream.builder()
        val totalDuration =
            Duration.between(points.first().time.orElse(Instant.EPOCH), points.last().time.orElse(Instant.EPOCH))
        if (totalDuration == Duration.ZERO) return speeds.build()

        val step: Int = max(100, (points.size / totalDuration.toMinutes())).toInt()

        for (i in 0 until points.size - 1) {
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
}