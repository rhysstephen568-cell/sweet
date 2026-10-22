package com.sweet.app.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sweet.app.core.crypto.SecretChatCryptoManager
import com.sweet.app.core.network.beam.SweetBeamManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val cryptoManager: SecretChatCryptoManager,
    private val beamManager: SweetBeamManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatId: String = savedStateHandle.get<String>("chatId") ?: "101"

    val messages: StateFlow<List<ChatMessage>> = chatRepository.getMessages(chatId)
    val isPeerTyping: StateFlow<Boolean> = chatRepository.isPeerTyping(chatId)

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isSecretMode = MutableStateFlow(chatId.contains("secret"))
    val isSecretMode: StateFlow<Boolean> = _isSecretMode.asStateFlow()

    private val _selfDestructSeconds = MutableStateFlow(0)
    val selfDestructSeconds: StateFlow<Int> = _selfDestructSeconds.asStateFlow()

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

    fun setSelfDestructSeconds(seconds: Int) {
        _selfDestructSeconds.value = seconds
    }

    fun toggleSecretMode() {
        _isSecretMode.value = !_isSecretMode.value
    }

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isNotEmpty()) {
            viewModelScope.launch {
                val finalContent = if (_isSecretMode.value) {
                    val (encrypted, iv) = cryptoManager.encryptMessage(content)
                    "[E2EE] " + android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
                } else {
                    content
                }

                chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = "usr_self_101",
                    senderName = "Me",
                    content = finalContent,
                    isFromMe = true
                )
                _inputText.value = ""
                chatRepository.sendTypingSignal(chatId, false)
            }
        }
    }

    fun sendBeamFile(file: File) {
        viewModelScope.launch {
            val task = beamManager.sendFileP2P("127.0.0.1", file)
            val beamMsg = ChatMessage(
                chatId = chatId,
                senderId = "usr_self_101",
                senderName = "Me",
                content = "SWEET Beam Transfer: ${file.name}",
                isFromMe = true,
                fileName = file.name,
                fileSizeStr = "${file.length() / (1024 * 1024)} MB",
                isBeamFile = true,
                transferProgressPercent = task.progressPercent,
                transferSpeedMBs = task.speedMBs,
                isTransferCompleted = true
            )
            chatRepository.sendMessage(
                chatId = chatId,
                senderId = "usr_self_101",
                senderName = "Me",
                content = beamMsg.content,
                isFromMe = true
            )
        }
    }

    fun deleteMessage(messageId: String) {
        // Self-destruct message deletion
    }
}
