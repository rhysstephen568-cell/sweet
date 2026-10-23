package com.sweet.app.feature.chat

data class ChatConversation(
    val chatId: String,
    val peerUserId: String,
    val actualPeerName: String,
    val customAlias: String? = null,
    val avatarUrl: String? = null,
    val lastMessageContent: String,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isPeerTyping: Boolean = false
) {
    val displayName: String get() = if (!customAlias.isNullOrBlank()) customAlias else actualPeerName
}
