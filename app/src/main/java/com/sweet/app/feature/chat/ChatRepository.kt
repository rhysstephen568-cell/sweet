package com.sweet.app.feature.chat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*
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
    fun addReaction(chatId: String, messageId: String, emoji: String)
    fun getCanonicalChatId(peerUserId: String): String
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val peerDiscoveryRepository: PeerDiscoveryRepository
) : ChatRepository {

    private val CHANNEL_ID = "sweet_chat_messages_channel"
    private val TCP_CHAT_PORT = 9876
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

        val initialMsg = ChatMessage(
            chatId = "101",
            senderId = "usr_support",
            senderName = "SWEET Support Team",
            content = "Welcome to SWEET messenger v0.01.1.9.2! Real-time medical consultation & P2P active.",
            isFromMe = false,
            status = MessageStatus.READ
        )
        getOrCreateFlow("101").value = listOf(initialMsg)
        updateConversationState("101", "usr_support", "SWEET Support Team", initialMsg.content, initialMsg.timestamp, incrementUnread = false)

        peerDiscoveryRepository.startDiscovery()
        startTcpChatServer()
        startUdpListener()
    }

    override fun getCanonicalChatId(peerUserId: String): String {
        val myUserId = peerDiscoveryRepository.getUniqueUserId()
        if (peerUserId == "101" || peerUserId.isBlank()) return "101"
        return if (myUserId <= peerUserId) "chat_${myUserId}_$peerUserId" else "chat_${peerUserId}_$myUserId"
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
        val canonicalId = getCanonicalChatId(chatId)
        return getOrCreateFlow(canonicalId).asStateFlow()
    }

    override fun isPeerTyping(chatId: String): StateFlow<Boolean> {
        val canonicalId = getCanonicalChatId(chatId)
        return getOrCreateTypingFlow(canonicalId).asStateFlow()
    }

    override suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        isFromMe: Boolean,
        replyToMsg: ChatMessage?
    ) {
        val canonicalId = getCanonicalChatId(chatId)
        val targetFlow = getOrCreateFlow(canonicalId)
        val defaultFlow = getOrCreateFlow("101")

        val newMsg = ChatMessage(
            chatId = canonicalId,
            senderId = senderId,
            senderName = senderName,
            content = content,
            isFromMe = isFromMe,
            status = MessageStatus.READ,
            replyToMessageId = replyToMsg?.id,
            replyToSenderName = replyToMsg?.senderName,
            replyToContent = replyToMsg?.content
        )

        targetFlow.value = targetFlow.value + newMsg
        if (canonicalId != "101") {
            defaultFlow.value = defaultFlow.value + newMsg
        }

        getOrCreateTypingFlow(canonicalId).value = false
        getOrCreateTypingFlow("101").value = false

        updateConversationState(canonicalId, senderId, senderName, content, newMsg.timestamp, incrementUnread = !isFromMe)

        if (isFromMe) {
            val replyPart = if (replyToMsg != null) "${replyToMsg.id}~#~${replyToMsg.senderName}~#~${replyToMsg.content}" else "none~#~none~#~none"
            broadcastPayload("MSG~#~$canonicalId~#~$senderId~#~$senderName~#~$content~#~$replyPart")
        }
    }

    override fun sendTypingSignal(chatId: String, isTyping: Boolean) {
        val canonicalId = getCanonicalChatId(chatId)
        val myUserId = peerDiscoveryRepository.getUniqueUserId()
        val myName = peerDiscoveryRepository.getDeviceModelName()
        val signal = if (isTyping) "TYPING_START" else "TYPING_STOP"
        broadcastPayload("$signal~#~$canonicalId~#~$myUserId~#~$myName~#~empty")
    }

    override fun togglePinConversation(chatId: String) {
        val canonicalId = getCanonicalChatId(chatId)
        val current = pinnedMap[canonicalId] ?: false
        pinnedMap[canonicalId] = !current
        refreshConversationsList()
    }

    override fun setCustomAlias(chatId: String, alias: String) {
        val canonicalId = getCanonicalChatId(chatId)
        if (alias.isBlank()) {
            customAliasMap.remove(canonicalId)
        } else {
            customAliasMap[canonicalId] = alias
        }
        refreshConversationsList()
    }

    override fun markChatAsRead(chatId: String) {
        val canonicalId = getCanonicalChatId(chatId)
        unreadMap[canonicalId] = 0
        refreshConversationsList()
    }

    override fun toggleStarMessage(chatId: String, messageId: String) {
        val canonicalId = getCanonicalChatId(chatId)
        val flow = getOrCreateFlow(canonicalId)
        flow.value = flow.value.map { msg ->
            if (msg.id == messageId) msg.copy(isStarred = !msg.isStarred) else msg
        }
    }

    override fun deleteMessage(chatId: String, messageId: String) {
        val canonicalId = getCanonicalChatId(chatId)
        val flow = getOrCreateFlow(canonicalId)
        flow.value = flow.value.map { msg ->
            if (msg.id == messageId) msg.copy(isDeleted = true, content = "This message was deleted") else msg
        }
    }

    override fun addReaction(chatId: String, messageId: String, emoji: String) {
        val canonicalId = getCanonicalChatId(chatId)
        val myUserId = peerDiscoveryRepository.getUniqueUserId()
        val flow = getOrCreateFlow(canonicalId)
        flow.value = flow.value.map { msg ->
            if (msg.id == messageId) {
                val updatedReactions = msg.reactions.toMutableMap()
                updatedReactions[myUserId] = emoji
                msg.copy(reactions = updatedReactions)
            } else msg
        }
        broadcastPayload("REACTION~#~$canonicalId~#~$messageId~#~$myUserId~#~$emoji")
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

    private fun getArpTableIps(): List<String> {
        val ips = mutableListOf<String>()
        runCatching {
            val file = File("/proc/net/arp")
            if (file.exists()) {
                file.forEachLine { line ->
                    val tokens = line.split(Regex("\\s+"))
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3]
                        if (ip.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) && mac != "00:00:00:00:00:00") {
                            ips.add(ip)
                        }
                    }
                }
            }
        }
        return ips
    }

    private fun broadcastPayload(payload: String) {
        scope.launch(Dispatchers.IO) {
            val myIp = getLocalIpAddress()
            val targetIps = mutableSetOf<String>()

            // Add active discovered peers
            peerDiscoveryRepository.peers.value.forEach { targetIps.add(it.host) }

            // Add ARP table IPs
            targetIps.addAll(getArpTableIps())

            // Add common local subnet IPs (192.168.1.2, 192.168.1.7, etc.)
            if (myIp != null && myIp.contains(".")) {
                val prefix = myIp.substringBeforeLast(".") + "."
                listOf(2, 3, 4, 5, 7, 8, 10, 15, 20, 100, 101, 102, 150, 200).forEach { lastOctet ->
                    targetIps.add("$prefix$lastOctet")
                }
            }

            // Direct TCP socket delivery
            targetIps.forEach { targetIp ->
                if (targetIp != myIp && targetIp != "127.0.0.1") {
                    launch {
                        sendTcpMessage(targetIp, payload)
                    }
                }
            }

            // Fallback UDP Broadcast
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val bytes = payload.toByteArray()

                runCatching {
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), TCP_CHAT_PORT)
                    socket.send(packet)
                }

                getSubnetBroadcastAddress()?.let { subnetBroadcast ->
                    runCatching {
                        val packet = DatagramPacket(bytes, bytes.size, subnetBroadcast, TCP_CHAT_PORT)
                        socket.send(packet)
                    }
                }

                socket.close()
            }
        }
    }

    private fun sendTcpMessage(targetIp: String, payload: String) {
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetIp, TCP_CHAT_PORT), 300)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                writer.write(payload + "\n")
                writer.flush()
                Log.d("SWEET_CHAT", "Direct TCP payload sent to $targetIp:$TCP_CHAT_PORT")
            }
        }
    }

    private fun startTcpChatServer() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(TCP_CHAT_PORT))
                }
                Log.d("SWEET_CHAT", "TCP Chat Server started on port $TCP_CHAT_PORT")

                while (isActive) {
                    val client = serverSocket.accept()
                    launch {
                        runCatching {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val line = reader.readLine()
                            if (!line.isNullOrBlank()) {
                                Log.d("SWEET_CHAT", "TCP Received payload: $line from ${client.inetAddress.hostAddress}")
                                handleIncomingPayload(line)
                            }
                            client.close()
                        }
                    }
                }
            }
        }
    }

    private fun startUdpListener() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(TCP_CHAT_PORT))
                }
                val buffer = ByteArray(8192)
                val myIp = getLocalIpAddress()

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val senderIp = packet.address.hostAddress

                    if (senderIp != null && senderIp != myIp && senderIp != "127.0.0.1") {
                        val receivedStr = String(packet.data, 0, packet.length)
                        Log.d("SWEET_CHAT", "UDP Received payload: $receivedStr from $senderIp")
                        handleIncomingPayload(receivedStr)
                    }
                }
            }
        }
    }

    private fun handleIncomingPayload(payload: String) {
        val parts = payload.trim().split("~#~")
        if (parts.size >= 4) {
            val header = parts[0]
            val chatId = parts[1]
            val senderId = parts[2]
            val senderName = parts[3]
            val myUserId = peerDiscoveryRepository.getUniqueUserId()

            if (senderId != myUserId) {
                val canonicalId = getCanonicalChatId(chatId)

                when (header) {
                    "TYPING_START" -> {
                        getOrCreateTypingFlow(canonicalId).value = true
                        getOrCreateTypingFlow("101").value = true
                        refreshConversationsList()
                    }
                    "TYPING_STOP" -> {
                        getOrCreateTypingFlow(canonicalId).value = false
                        getOrCreateTypingFlow("101").value = false
                        refreshConversationsList()
                    }
                    "REACTION" -> {
                        if (parts.size >= 5) {
                            val messageId = parts[2]
                            val emoji = parts[4]
                            val flow = getOrCreateFlow(canonicalId)
                            flow.value = flow.value.map { msg ->
                                if (msg.id == messageId) {
                                    val updatedMap = msg.reactions.toMutableMap()
                                    updatedMap[senderId] = emoji
                                    msg.copy(reactions = updatedMap)
                                } else msg
                            }
                        }
                    }
                    "MSG" -> {
                        val content = if (parts.size >= 5) parts[4] else ""
                        val targetFlow = getOrCreateFlow(canonicalId)
                        val defaultFlow = getOrCreateFlow("101")

                        var replyId: String? = null
                        var replyName: String? = null
                        var replyContent: String? = null

                        if (parts.size >= 6) {
                            val replyPayload = parts[5].split("|")
                            if (replyPayload.size >= 3 && replyPayload[0] != "none") {
                                replyId = replyPayload[0]
                                replyName = replyPayload[1]
                                replyContent = replyPayload[2]
                            }
                        }

                        val incomingMsg = ChatMessage(
                            chatId = canonicalId,
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
                        if (canonicalId != "101") {
                            defaultFlow.value = defaultFlow.value + incomingMsg
                        }

                        getOrCreateTypingFlow(canonicalId).value = false
                        getOrCreateTypingFlow("101").value = false

                        val displayPeerName = customAliasMap[canonicalId] ?: senderName
                        updateConversationState(canonicalId, senderId, displayPeerName, content, incomingMsg.timestamp, incrementUnread = true)
                        showIncomingMessageNotification(displayPeerName, content)
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
