package com.sweet.app.feature.chat

import android.content.Context
import android.net.wifi.WifiManager
import com.sweet.app.core.network.nearby.PeerDiscoveryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Inet4Address
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    fun getMessages(chatId: String): StateFlow<List<ChatMessage>>
    fun isPeerTyping(chatId: String): StateFlow<Boolean>
    suspend fun sendMessage(chatId: String, senderId: String, senderName: String, content: String, isFromMe: Boolean)
    fun sendTypingSignal(chatId: String, isTyping: Boolean)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerDiscoveryRepository: PeerDiscoveryRepository
) : ChatRepository {

    private val messagesMap = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val typingMap = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val PORT = 9876

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    init {
        val initialMsg = ChatMessage(
            chatId = "101",
            senderId = "usr_support",
            senderName = "SWEET Support Team",
            content = "Welcome to SWEET messenger v0.01.1.8! Real-time local Wi-Fi chat active.",
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
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val bytes = payload.toByteArray()

                // 1. Broadcast to 255.255.255.255
                runCatching {
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), PORT)
                    socket.send(packet)
                }

                // 2. Broadcast to Subnet Broadcast Address
                getSubnetBroadcastAddress()?.let { subnetBroadcast ->
                    runCatching {
                        val packet = DatagramPacket(bytes, bytes.size, subnetBroadcast, PORT)
                        socket.send(packet)
                    }
                }

                // 3. Direct Unicast to all discovered active peer IPs
                val activePeers = peerDiscoveryRepository.peers.value
                activePeers.forEach { peer ->
                    runCatching {
                        val targetAddr = InetAddress.getByName(peer.host)
                        val packet = DatagramPacket(bytes, bytes.size, targetAddr, PORT)
                        socket.send(packet)
                    }
                }

                socket.close()
            }
        }
    }

    private fun startUdpListener() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(PORT))
                }
                val buffer = ByteArray(4096)
                val myIp = getLocalIpAddress()

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val senderIp = packet.address.hostAddress

                    if (senderIp != null && senderIp != myIp && senderIp != "127.0.0.1") {
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

    private fun getSubnetBroadcastAddress(): InetAddress? {
        return try {
            @Suppress("DEPRECATION")
            val dhcp = wifiManager.dhcpInfo ?: return null
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) {
                quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
            }
            InetAddress.getByAddress(quads)
        } catch (_: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
