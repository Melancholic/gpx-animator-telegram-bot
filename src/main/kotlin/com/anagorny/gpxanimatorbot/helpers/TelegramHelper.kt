package com.anagorny.gpxanimatorbot.helpers

import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient

fun TelegramClient.sentAction(chatId: Long, action: ActionType) {
    execute(
        SendChatAction.builder()
            .chatId(chatId)
            .action(action.toString())
            .build()
    )
}

fun TelegramClient.deleteMessage(message: Message) {
    execute(
        DeleteMessage.builder()
            .chatId(message.chatId.toString())
            .messageId(message.messageId)
            .build()
    )
}
