package com.anagorny.gpxanimatorbot.helpers

import org.slf4j.Logger
import org.telegram.telegrambots.meta.api.objects.InputFile
import java.io.File

fun loadFile(file: File, originalName: String? = null, logger: Logger): InputFile {
    return try {
        InputFile(file, originalName)
    } catch (e: Exception) {
        logger.error("Cant read file '${file.absolutePath}' fo sending", e)
        throw e
    }
}

fun removeFile(file: File, logger: Logger) {
    try {
        val path = file.absolutePath
        if (file.delete()) {
            logger.info("File $path deleted")
        } else {
            throw Exception("File $path cant be deleted")
        }
    } catch (e: Exception) {
        logger.error("Error while removing file", e)
    }
}

fun removeFile(filePath: String, logger: Logger) {
    try {
        return removeFile(File(filePath), logger)
    } catch (e: Exception) {
        logger.error("Error while remove file '$filePath'", e)
    }
}

fun removeFileIfExist(filePath: String?, logger: Logger) {
    if (!filePath.isNullOrBlank()) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.delete()) {
                logger.info("File $filePath deleted successfully")
            } else {
                throw Exception("file $filePath cant be deleted")
            }
        }
    }
}