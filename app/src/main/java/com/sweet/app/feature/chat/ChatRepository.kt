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
    suspend fun sendMessage(chatId: String, senderId: String, senderName: String, content: String, isFromMe: Boolean)
}

@Singleton
class ChatRepositoryImpl @Inject constructor() : ChatRepository {

    private val messagesMap = ConcurrentHashMapState()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val PORT = 9876

    init {
        // Initial sample support message
        val initialMsg = ChatMessage(
            chatId = "101",
            senderId = "usr_support",
            senderName = "SWEET Support Team",
            content = "Welcome to SWEET messenger v0.01.1.2! Your local & cloud chat pipeline is active.",
            isFromMe = false,
            status = MessageStatus.READ
        )
        getOrCreateFlow("101").value = listOf(initialMsg)

        // Listen for UDP Broadcast messages on local Wi-Fi subnet
        startUdpListener()
    }

    private fun getOrCreateFlow(chatId: String): MutableStateFlow<List<ChatMessage>> {
        return messagesMap.getOrPut(chatId) { MutableStateFlow(emptyList()) }
    }

    override fun getMessages(chatId: String): StateFlow<List<ChatMessage>> {
        return getOrCreateFlow(chatId).asStateFlow()
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

        // Broadcast message over local Wi-Fi subnet so peer device receives it instantly
        if (isFromMe) {
            broadcastMessage(chatId, senderId, senderName, content)
        }
    }

    private fun broadcastMessage(chatId: String, senderId: String, senderName: String, content: String) {
        scope.launch {
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val payload = "$chatId|$senderId|$senderName|$content"
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
                        val chatId = parts[0]
                        val senderId = parts[1]
                        val senderName = parts[2]
                        val content = parts[3]

                        if (senderId != "usr_self_101") {
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
                        }
                    }
                }
            }
        }
    }
}

// Concurrent Map Wrapper
private class ConcurrentHashMapState {
    private val map = java.util.concurrent.ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    fun getOrPut(key: String, defaultValue: () -> MutableStateFlow<List<ChatMessage>>): MutableStateFlow<List<ChatMessage>> {
        return map.getOrPut(key, defaultValue)
    }
}
