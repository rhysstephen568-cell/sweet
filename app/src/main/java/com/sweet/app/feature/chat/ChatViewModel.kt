package com.sweet.app.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatId: String = savedStateHandle.get<String>("chatId") ?: "101"

    val messages: StateFlow<List<ChatMessage>> = chatRepository.getMessages(chatId)
    val isPeerTyping: StateFlow<Boolean> = chatRepository.isPeerTyping(chatId)

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var typingJob: Job? = null

    fun onInputTextChanged(text: String) {
        _inputText.value = text

        if (text.isNotBlank()) {
            chatRepository.sendTypingSignal(chatId, true)
            typingJob?.cancel()
            typingJob = viewModelScope.launch {
                delay(3000)
                chatRepository.sendTypingSignal(chatId, false)
            }
        } else {
            chatRepository.sendTypingSignal(chatId, false)
        }
    }

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = "usr_self_101",
                    senderName = "Me",
                    content = content,
                    isFromMe = true
                )
                _inputText.value = ""
                chatRepository.sendTypingSignal(chatId, false)
            }
        }
    }
}
