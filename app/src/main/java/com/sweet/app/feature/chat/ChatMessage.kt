package com.sweet.app.feature.chat

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT
)
