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
    val status: MessageStatus = MessageStatus.SENT,
    val isSecret: Boolean = false,
    val selfDestructSeconds: Int = 0,
    val fileName: String? = null,
    val fileSizeStr: String? = null,
    val isBeamFile: Boolean = false,
    val transferProgressPercent: Float = 0f,
    val transferSpeedMBs: Float = 0f,
    val isTransferCompleted: Boolean = false
)
