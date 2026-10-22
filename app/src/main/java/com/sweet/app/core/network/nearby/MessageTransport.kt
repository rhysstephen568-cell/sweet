package com.sweet.app.core.network.nearby

import javax.inject.Inject
import javax.inject.Singleton

interface MessageTransport {
    suspend fun sendMessage(chatId: String, clientMessageId: String, content: String): Boolean
    fun isAvailable(): Boolean
}

@Singleton
class CloudTransport @Inject constructor() : MessageTransport {
    override suspend fun sendMessage(chatId: String, clientMessageId: String, content: String): Boolean {
        // Cloud transport (Firebase / Retrofit WebSocket fallback)
        return true
    }

    override fun isAvailable(): Boolean = true
}

@Singleton
class LocalTransport @Inject constructor() : MessageTransport {
    override suspend fun sendMessage(chatId: String, clientMessageId: String, content: String): Boolean {
        // Direct local subnet TCP socket transport
        return true
    }

    override fun isAvailable(): Boolean = false
}

@Singleton
class TransportSelector @Inject constructor(
    private val cloudTransport: CloudTransport,
    private val localTransport: LocalTransport,
    private val peerDiscoveryRepository: PeerDiscoveryRepository
) {
    fun selectTransport(recipientUserId: String): MessageTransport {
        val activePeers = peerDiscoveryRepository.peers.value
        val isVisibleOnLan = activePeers.any { it.userId == recipientUserId }

        return if (isVisibleOnLan && localTransport.isAvailable()) {
            localTransport
        } else {
            cloudTransport
        }
    }
}
