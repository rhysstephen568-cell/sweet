package com.sweet.app.feature.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sweet.app.MainActivity
import com.sweet.app.R
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
    val conversations: StateFlow<List<ChatConversation>>
    fun getMessages(chatId: String): StateFlow<List<ChatMessage>>
    fun isPeerTyping(chatId: String): StateFlow<Boolean>
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        isFromMe: Boolean,
        replyToMsg: ChatMessage? = null
    )
    fun sendTypingSignal(chatId: String, isTyping: Boolean)
    fun togglePinConversation(chatId: String)
    fun setCustomAlias(chatId: String, alias: String)
    fun markChatAsRead(chatId: String)
    fun toggleStarMessage(chatId: String, messageId: String)
    fun deleteMessage(chatId: String, messageId: String)
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerDiscoveryRepository: PeerDiscoveryRepository
) : ChatRepository {

    private val CHANNEL_ID = "sweet_chat_messages_channel"
    private val PORT = 9876
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val messagesMap = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val typingMap = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    override val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val customAliasMap = ConcurrentHashMap<String, String>()
    private val pinnedMap = ConcurrentHashMap<String, Boolean>()
    private val unreadMap = ConcurrentHashMap<String, Int>()

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    init {
        createNotificationChannel()

        // Seed initial support conversation
        val initialMsg = ChatMessage(
            chatId = "101",
            senderId = "usr_support",
            senderName = "SWEET Support Team",
            content = "Welcome to SWEET messenger v0.01.1.9.1! Real-time WhatsApp/Telegram style local Wi-Fi chat active.",
            isFromMe = false,
            status = MessageStatus.READ
        )
        getOrCreateFlow("101").value = listOf(initialMsg)
        updateConversationState("101", "usr_support", "SWEET Support Team", initialMsg.content, initialMsg.timestamp, incrementUnread = false)

        startUdpListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SWEET Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for incoming SWEET chat messages"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
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

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        isFromMe: Boolean,
        replyToMsg: ChatMessage?
    ) {
        val targetFlow = getOrCreateFlow(chatId)
        val defaultFlow = getOrCreateFlow("101")

        val newMsg = ChatMessage(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            isFromMe = isFromMe,
            status = MessageStatus.SENT,
            replyToMessageId = replyToMsg?.id,
            replyToSenderName = replyToMsg?.senderName,
            replyToContent = replyToMsg?.content
        )

        targetFlow.value = targetFlow.value + newMsg
        if (chatId != "101") {
            defaultFlow.value = defaultFlow.value + newMsg
        }

        getOrCreateTypingFlow(chatId).value = false
        updateConversationState(chatId, senderId, senderName, content, newMsg.timestamp, incrementUnread = !isFromMe)

        if (isFromMe) {
            val replyPart = if (replyToMsg != null) "${replyToMsg.id}|${replyToMsg.senderName}|${replyToMsg.content}" else "none|none|none"
            broadcastPayload("MSG|$chatId|$senderId|$senderName|$content|$replyPart")
        }
    }

    override fun sendTypingSignal(chatId: String, isTyping: Boolean) {
        val myUserId = peerDiscoveryRepository.getUniqueUserId()
        val myName = peerDiscoveryRepository.getDeviceModelName()
        val signal = if (isTyping) "TYPING_START" else "TYPING_STOP"
        broadcastPayload("$signal|$chatId|$myUserId|$myName|empty")
    }

    override fun togglePinConversation(chatId: String) {
        val current = pinnedMap[chatId] ?: false
        pinnedMap[chatId] = !current
        refreshConversationsList()
    }

    override fun setCustomAlias(chatId: String, alias: String) {
        if (alias.isBlank()) {
            customAliasMap.remove(chatId)
        } else {
            customAliasMap[chatId] = alias
        }
        refreshConversationsList()
    }

    override fun markChatAsRead(chatId: String) {
        unreadMap[chatId] = 0
        refreshConversationsList()
    }

    override fun toggleStarMessage(chatId: String, messageId: String) {
        val flow = getOrCreateFlow(chatId)
        flow.value = flow.value.map { msg ->
            if (msg.id == messageId) msg.copy(isStarred = !msg.isStarred) else msg
        }
    }

    override fun deleteMessage(chatId: String, messageId: String) {
        val flow = getOrCreateFlow(chatId)
        flow.value = flow.value.map { msg ->
            if (msg.id == messageId) msg.copy(isDeleted = true, content = "This message was deleted") else msg
        }
    }

    private fun updateConversationState(
        chatId: String,
        peerUserId: String,
        peerName: String,
        lastMessage: String,
        timestamp: Long,
        incrementUnread: Boolean
    ) {
        if (incrementUnread) {
            unreadMap[chatId] = (unreadMap[chatId] ?: 0) + 1
        }

        val alias = customAliasMap[chatId]
        val isPinned = pinnedMap[chatId] ?: false
        val unread = unreadMap[chatId] ?: 0
        val isTyping = getOrCreateTypingFlow(chatId).value

        val conversation = ChatConversation(
            chatId = chatId,
            peerUserId = peerUserId,
            actualPeerName = peerName,
            customAlias = alias,
            lastMessageContent = lastMessage,
            lastMessageTimestamp = timestamp,
            unreadCount = unread,
            isPinned = isPinned,
            isPeerTyping = isTyping
        )

        val currentList = _conversations.value.toMutableList()
        currentList.removeAll { it.chatId == chatId }
        currentList.add(conversation)

        _conversations.value = sortConversations(currentList)
    }

    private fun refreshConversationsList() {
        _conversations.value = sortConversations(_conversations.value)
    }

    private fun sortConversations(list: List<ChatConversation>): List<ChatConversation> {
        return list.map { conv ->
            conv.copy(
                customAlias = customAliasMap[conv.chatId],
                isPinned = pinnedMap[conv.chatId] ?: false,
                unreadCount = unreadMap[conv.chatId] ?: 0,
                isPeerTyping = getOrCreateTypingFlow(conv.chatId).value
            )
        }.sortedWith(
            compareByDescending<ChatConversation> { it.isPinned }
                .thenByDescending { it.lastMessageTimestamp }
        )
    }

    private fun broadcastPayload(payload: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val bytes = payload.toByteArray()

                runCatching {
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), PORT)
                    socket.send(packet)
                }

                getSubnetBroadcastAddress()?.let { subnetBroadcast ->
                    runCatching {
                        val packet = DatagramPacket(bytes, bytes.size, subnetBroadcast, PORT)
                        socket.send(packet)
                    }
                }

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
                    val myUserId = peerDiscoveryRepository.getUniqueUserId()

                    if (senderIp != null && senderIp != myIp && senderIp != "127.0.0.1") {
                        val receivedStr = String(packet.data, 0, packet.length)
                        val parts = receivedStr.split("|")

                        if (parts.size >= 4) {
                            val header = parts[0]
                            val chatId = parts[1]
                            val senderId = parts[2]
                            val senderName = parts[3]

                            if (senderId != myUserId) {
                                when (header) {
                                    "TYPING_START" -> {
                                        getOrCreateTypingFlow(chatId).value = true
                                        getOrCreateTypingFlow("101").value = true
                                        refreshConversationsList()
                                    }
                                    "TYPING_STOP" -> {
                                        getOrCreateTypingFlow(chatId).value = false
                                        getOrCreateTypingFlow("101").value = false
                                        refreshConversationsList()
                                    }
                                    "MSG" -> {
                                        val content = if (parts.size >= 5) parts[4] else ""
                                        val targetFlow = getOrCreateFlow(chatId)
                                        val defaultFlow = getOrCreateFlow("101")

                                        var replyId: String? = null
                                        var replyName: String? = null
                                        var replyContent: String? = null

                                        if (parts.size >= 8 && parts[5] != "none") {
                                            replyId = parts[5]
                                            replyName = parts[6]
                                            replyContent = parts[7]
                                        }

                                        val incomingMsg = ChatMessage(
                                            chatId = chatId,
                                            senderId = senderId,
                                            senderName = senderName,
                                            content = content,
                                            isFromMe = false,
                                            status = MessageStatus.READ,
                                            replyToMessageId = replyId,
                                            replyToSenderName = replyName,
                                            replyToContent = replyContent
                                        )

                                        targetFlow.value = targetFlow.value + incomingMsg
                                        if (chatId != "101") {
                                            defaultFlow.value = defaultFlow.value + incomingMsg
                                        }

                                        getOrCreateTypingFlow(chatId).value = false
                                        getOrCreateTypingFlow("101").value = false

                                        val displayPeerName = customAliasMap[chatId] ?: senderName
                                        updateConversationState(chatId, senderId, displayPeerName, content, incomingMsg.timestamp, incrementUnread = true)
                                        showIncomingMessageNotification(displayPeerName, content)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showIncomingMessageNotification(senderName: String, content: String) {
        runCatching {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(senderName)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify((1000..9999).random(), builder.build())
                }
            } else {
                notificationManager.notify((1000..9999).random(), builder.build())
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
