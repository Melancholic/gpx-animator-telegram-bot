package com.anagorny.gpxanimatorbot.handlers

import org.telegram.telegrambots.meta.api.objects.Update

interface UpdatesHandler {
    suspend fun handle(update: Update)
}