package com.sweet.app.feature.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    fun getMessages(chatId: String): StateFlow<List<ChatMessage>>
    fun isPeerTyping(chatId: String): StateFlow<Boolean>
    suspend fun sendMessage(chatId: String, senderId: String, senderName: String, content: String, isFromMe: Boolean)
    fun sendTypingSignal(chatId: String, isTyping: Boolean)
}

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val messagesMap = ConcurrentHashMapState()
    private val typingMap = ConcurrentHashMapTypingState()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val PORT = 9876

    init {
        val initialMsg = ChatMessage(
            chatId = "101",
            senderId = "usr_support",
            senderName = "SWEET Support Team",
            content = "Welcome to SWEET messenger v0.01.1.3! Real-time typing & discovery active.",
            isFromMe = false,
            status = MessageStatus.READ
        )
        getOrCreateFlow("101").value = listOf(initialMsg)

        startUdpListener()
    }

    private fun getOrCreateFlow(chatId: String): MutableStateFlow<List<ChatMessage>> {
        return messagesMap.getOrPut(chatId) { MutableStateFlow(emptyList()) }
    }

    private fun getOrCreateTypingFlow(chatId: String): MutableStateFlow<Boolean> {
        return typingMap.getOrPut(chatId) { MutableStateFlow(false) }
    }

    override fun getMessages(chatId: String): StateFlow<List<ChatMessage>> {
        return getOrCreateFlow(chatId).asStateFlow()
    }

    override fun isPeerTyping(chatId: String): StateFlow<Boolean> {
        return getOrCreateTypingFlow(chatId).asStateFlow()
    }

    override suspend fun sendMessage(chatId: String, senderId: String, senderName: String, content: String, isFromMe: Boolean) {
        val flow = getOrCreateFlow(chatId)
        val newMsg = ChatMessage(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            isFromMe = isFromMe,
            status = MessageStatus.SENT
        )
        flow.value = flow.value + newMsg

        // Stop typing indicator when message is sent
        getOrCreateTypingFlow(chatId).value = false

        if (isFromMe) {
            broadcastPayload("MSG|$chatId|$senderId|$senderName|$content")
        }
    }

    override fun sendTypingSignal(chatId: String, isTyping: Boolean) {
        val signal = if (isTyping) "TYPING_START" else "TYPING_STOP"
        broadcastPayload("$signal|$chatId|usr_self_101|Me|empty")
    }

    private fun broadcastPayload(payload: String) {
        scope.launch {
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val bytes = payload.toByteArray()
                val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), PORT)
                socket.send(packet)
                socket.close()
            }
        }
    }

    private fun startUdpListener() {
        scope.launch {
            runCatching {
                val socket = DatagramSocket(PORT)
                val buffer = ByteArray(2048)
                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val receivedStr = String(packet.data, 0, packet.length)
                    val parts = receivedStr.split("|")

                    if (parts.size >= 4) {
                        val header = parts[0]
                        val chatId = parts[1]
                        val senderId = parts[2]
                        val senderName = parts[3]

                        if (senderId != "usr_self_101") {
                            when (header) {
                                "TYPING_START" -> {
                                    getOrCreateTypingFlow(chatId).value = true
                                }
                                "TYPING_STOP" -> {
                                    getOrCreateTypingFlow(chatId).value = false
                                }
                                "MSG" -> {
                                    val content = if (parts.size >= 5) parts[4] else ""
                                    val flow = getOrCreateFlow(chatId)
                                    val incomingMsg = ChatMessage(
                                        chatId = chatId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        content = content,
                                        isFromMe = false,
                                        status = MessageStatus.READ
                                    )
                                    flow.value = flow.value + incomingMsg
                                    getOrCreateTypingFlow(chatId).value = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private class ConcurrentHashMapState {
    private val map = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    fun getOrPut(key: String, defaultValue: () -> MutableStateFlow<List<ChatMessage>>): MutableStateFlow<List<ChatMessage>> {
        return map.getOrPut(key, defaultValue)
    }
}

private class ConcurrentHashMapTypingState {
    private val map = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    fun getOrPut(key: String, defaultValue: () -> MutableStateFlow<Boolean>): MutableStateFlow<Boolean> {
        return map.getOrPut(key, defaultValue)
    }
}
