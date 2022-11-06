package com.anagorny.gpxanimatorbot.services.impl

import com.anagorny.gpxanimatorbot.config.GpxAnimatorAppProperties
import com.anagorny.gpxanimatorbot.helpers.io
import com.anagorny.gpxanimatorbot.helpers.runAsync
import com.anagorny.gpxanimatorbot.model.GPXAnalyzeResult
import com.anagorny.gpxanimatorbot.services.GPXAnalyzeService
import com.anagorny.gpxanimatorbot.services.GpxAnimatorRunner
import com.anagorny.gpxanimatorbot.services.GpxProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import kotlin.io.path.absolutePathString

@Service
class GpxProcessorIml(
    @Qualifier("mainFlowCoroutineScope")
    private val scope: CoroutineScope,
    private val gpxAnalyzeService: GPXAnalyzeService,
    private val gpxAnimatorRunner: GpxAnimatorRunner,
    private val gpxAnimatorAppProperties: GpxAnimatorAppProperties,
) : GpxProcessor {

    override suspend fun asyncAnalyzeGpxFile(file: File) = scope.runAsync { gpxAnalyzeService.doAnalyze(file) }
    override suspend fun asyncRenderVideo(file: File) = scope.runAsync {
        val outFilePath = io {
            Files.createTempFile(null, ".${gpxAnimatorAppProperties.outputFormat.ext}").absolutePathString()
        }
        gpxAnimatorRunner.run(file.absolutePath, outFilePath)
    }

    override suspend fun doProcess(file: File): Pair<GPXAnalyzeResult, File> {
        val gpxAnalyzeResultDeferred = asyncAnalyzeGpxFile(file)
        val gpxAnimatorRunningResultDeferred: Deferred<File> = asyncRenderVideo(file)

        return gpxAnalyzeResultDeferred.await() to gpxAnimatorRunningResultDeferred.await()
    }
}
