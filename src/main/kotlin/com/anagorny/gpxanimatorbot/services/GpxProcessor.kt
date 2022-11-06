package com.anagorny.gpxanimatorbot.services

import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import kotlinx.coroutines.Deferred
import java.io.File

interface GpxProcessor {
    suspend fun asyncAnalyzeGpxFile(file: File): Deferred<GPXAnalyzeResult>
    suspend fun asyncRenderVideo(file: File): Deferred<File>
    suspend fun doProcess(file: File): Pair<GPXAnalyzeResult, File>
}
