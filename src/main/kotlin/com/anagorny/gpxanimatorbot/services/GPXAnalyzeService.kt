package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import io.jenetics.jpx.GPX
import io.jenetics.jpx.Length
import io.jenetics.jpx.Point
import io.jenetics.jpx.WayPoint
import java.io.File
import java.time.Duration
import java.util.stream.Stream

interface GPXAnalyzeService {
    fun readGPX(file: File): GPX
    fun startPoint(gpx: GPX): Point?
    fun finishPoint(gpx: GPX): Point?
    fun totalDistance(gpx: GPX): Length?
    suspend fun doAnalyze(file: File): GPXAnalyzeResult
    fun getAllPoints(gpx: GPX): List<WayPoint>
    fun getAllPointsAsStream(gpx: GPX): Stream<WayPoint>
    fun totalTime(gpx: GPX): Duration
    fun totalAscending(gpx: GPX): Double?
    fun totalDescending(gpx: GPX): Double?
}