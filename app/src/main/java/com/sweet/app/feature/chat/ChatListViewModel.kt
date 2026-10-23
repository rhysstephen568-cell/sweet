package com.sweet.app.feature.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val conversations: StateFlow<List<ChatConversation>> = chatRepository.conversations

    fun togglePinConversation(chatId: String) {
        chatRepository.togglePinConversation(chatId)
    }

    fun setCustomAlias(chatId: String, alias: String) {
        chatRepository.setCustomAlias(chatId, alias)
    }

    fun markChatAsRead(chatId: String) {
        chatRepository.markChatAsRead(chatId)
    }
}
