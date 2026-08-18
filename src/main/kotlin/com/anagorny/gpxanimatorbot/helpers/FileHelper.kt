package com.anagorny.gpxanimatorbot.helpers

import io.github.oshai.kotlinlogging.KLogger
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.File

fun loadFile(file: File, originalName: String? = null, logger: KLogger): InputFile {
    return try {
        InputFile(file, originalName)
    } catch (e: Exception) {
        logger.error(e) { "Cant read file '${file.absolutePath}' fo sending" }
        throw e
    }
}

fun removeFile(file: File, logger: KLogger) {
    try {
        val path = file.absolutePath
        if (file.delete()) {
            logger.info { "File $path deleted" }
        } else {
            throw Exception("File $path cant be deleted")
        }
    } catch (e: Exception) {
        logger.error(e) { "Error while removing file" }
    }
}

fun removeFile(filePath: String, logger: KLogger) {
    try {
        return removeFile(File(filePath), logger)
    } catch (e: Exception) {
        logger.error(e) { "Error while remove file '$filePath'" }
    }
}

fun removeFileIfExist(filePath: String?, logger: KLogger) {
    if (!filePath.isNullOrBlank()) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.delete()) {
                logger.info { "File $filePath deleted successfully" }
            } else {
                throw Exception("file $filePath cant be deleted")
            }
        }
    }
}
