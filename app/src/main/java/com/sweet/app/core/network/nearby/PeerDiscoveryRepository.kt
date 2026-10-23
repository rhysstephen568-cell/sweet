package com.sweet.app.core.network.nearby

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*
import java.util.Locale
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

interface PeerDiscoveryRepository {
    val peers: StateFlow<List<NearbyPeer>>
    val isDiscovering: StateFlow<Boolean>
    fun getUniqueUserId(): String
    fun getDeviceModelName(): String
    fun startDiscovery(currentUserId: String = getUniqueUserId(), displayName: String = getDeviceModelName())
    fun stopDiscovery()
}

@Singleton
class NsdPeerDiscoveryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PeerDiscoveryRepository {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _peers = MutableStateFlow<List<NearbyPeer>>(emptyList())
    override val peers: StateFlow<List<NearbyPeer>> = _peers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceInfo: NsdServiceInfo? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var tcpServerJob: Job? = null
    private var subnetScannerJob: Job? = null
    private var cleanupJob: Job? = null

    private val SERVICE_TYPE = "_sweet._tcp."
    private val DISCOVERY_PORT = 9877
    private val HANDSHAKE_PORT = 9878

    override fun getUniqueUserId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull() ?: Build.FINGERPRINT
        val hash = abs((androidId + Build.MODEL).hashCode())
        return "usr_${Build.MODEL.replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.ROOT)}_$hash"
    }

    override fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.ROOT).startsWith(manufacturer.lowercase(Locale.ROOT))) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} $model"
        }
    }

    private fun resolveDeviceModelName(targetIp: String): String {
        return try {
            val inetAddr = InetAddress.getByName(targetIp)
            val canonicalHostName = inetAddr.canonicalHostName
            val hostName = inetAddr.hostName
            val name = when {
                !canonicalHostName.equals(targetIp, ignoreCase = true) && canonicalHostName.isNotBlank() -> canonicalHostName
                !hostName.equals(targetIp, ignoreCase = true) && hostName.isNotBlank() -> hostName
                else -> "Device (${targetIp.substringAfterLast(".")})"
            }
            name.removeSuffix(".local").removeSuffix(".home").removeSuffix(".lan")
        } catch (_: Exception) {
            "Device (${targetIp.substringAfterLast(".")})"
        }
    }

    private fun addOrUpdatePeer(newPeer: NearbyPeer, myUserId: String) {
        val myIp = getLocalIpAddress()
        // Filter out self by ID and IP address
        if (newPeer.userId == myUserId || newPeer.host == myIp || newPeer.host == "127.0.0.1") {
            return
        }

        val currentList = _peers.value.toMutableList()
        currentList.removeAll { existing ->
            existing.userId == newPeer.userId ||
            existing.host == newPeer.host
        }

        currentList.add(newPeer)
        _peers.value = deduplicatePeers(currentList, myUserId)
    }

    private fun deduplicatePeers(peersList: List<NearbyPeer>, myUserId: String): List<NearbyPeer> {
        val myIp = getLocalIpAddress()
        return peersList
            .filterNot { it.userId == myUserId || it.host == myIp || it.host == "127.0.0.1" }
            .groupBy { peer ->
                if (peer.isSweetUser) peer.userId else peer.host
            }
            .map { (_, group) ->
                group.maxByOrNull { (if (it.isSweetUser) 10000 else 0) + it.lastSeenTimestamp }!!
            }
            .distinctBy { it.host }
    }

    override fun startDiscovery(currentUserId: String, displayName: String) {
        val myUserId = if (currentUserId.isBlank() || currentUserId == "usr_self_101") getUniqueUserId() else currentUserId
        val myModelName = if (displayName.isBlank() || displayName == "Me (You)") getDeviceModelName() else displayName

        if (_isDiscovering.value) return

        runCatching {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("SweetMulticastLock").apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
        }

        serviceInfo = NsdServiceInfo().apply {
            serviceName = "SWEET_$myUserId"
            serviceType = SERVICE_TYPE
            port = HANDSHAKE_PORT
            setAttribute("userId", myUserId)
            setAttribute("displayName", myModelName)
            setAttribute("discoverable", "true")
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {}
            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(arg0: NsdServiceInfo, arg1: Int) {}
        }

        runCatching {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isDiscovering.value = false
            }

            override fun onDiscoveryStarted(serviceType: String) {
                _isDiscovering.value = true
            }

            override fun onDiscoveryStopped(serviceType: String) {
                _isDiscovering.value = false
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_sweet") && !service.serviceName.contains(myUserId)) {
                    resolveService(service, myUserId)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val peerId = service.serviceName.removePrefix("SWEET_")
                removePeer(peerId)
            }
        }

        runCatching {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }

        _isDiscovering.value = true

        startTcpDiscoveryServer(myUserId, myModelName)
        startUdpHeartbeat(myUserId, myModelName)
        startSubnetScanner(myUserId)

        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                _peers.value = _peers.value.filter { now - it.lastSeenTimestamp < 25000 }
            }
        }
    }

    private fun startTcpDiscoveryServer(myUserId: String, displayName: String) {
        tcpServerJob?.cancel()
        tcpServerJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(HANDSHAKE_PORT))
                }
                while (isActive) {
                    val client = serverSocket.accept()
                    launch {
                        runCatching {
                            val reader = client.getInputStream().bufferedReader()
                            val writer = client.getOutputStream().bufferedWriter()
                            val line = reader.readLine()
                            if (line?.startsWith("SWEET_PING") == true) {
                                writer.write("SWEET_PONG|$myUserId|$displayName\n")
                                writer.flush()
                            }
                            client.close()
                        }
                    }
                }
            }
        }
    }

    private fun startUdpHeartbeat(myUserId: String, displayName: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            launch {
                while (isActive) {
                    runCatching {
                        val socket = DatagramSocket()
                        socket.broadcast = true
                        val payload = "HEARTBEAT|$myUserId|$displayName"
                        val bytes = payload.toByteArray()

                        runCatching {
                            val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
                            socket.send(packet)
                        }

                        getSubnetBroadcastAddress()?.let { subnetBroadcast ->
                            runCatching {
                                val packet = DatagramPacket(bytes, bytes.size, subnetBroadcast, DISCOVERY_PORT)
                                socket.send(packet)
                            }
                        }

                        socket.close()
                    }
                    delay(2000)
                }
            }

            launch {
                runCatching {
                    val socket = DatagramSocket(null).apply {
                        reuseAddress = true
                        bind(InetSocketAddress(DISCOVERY_PORT))
                    }
                    val buffer = ByteArray(1024)
                    val myIp = getLocalIpAddress()

                    while (isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val packetIp = packet.address.hostAddress

                        if (packetIp != null && packetIp != myIp && packetIp != "127.0.0.1") {
                            val receivedStr = String(packet.data, 0, packet.length)
                            val parts = receivedStr.split("|")
                            if (parts.size >= 3 && parts[0] == "HEARTBEAT") {
                                val userId = parts[1]
                                val name = parts[2]

                                if (userId != myUserId) {
                                    val newPeer = NearbyPeer(
                                        userId = userId,
                                        name = name,
                                        host = packetIp,
                                        port = HANDSHAKE_PORT,
                                        rttMs = 10L,
                                        lastSeenTimestamp = System.currentTimeMillis(),
                                        isSweetUser = true
                                    )
                                    addOrUpdatePeer(newPeer, myUserId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startSubnetScanner(myUserId: String) {
        subnetScannerJob?.cancel()
        subnetScannerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val myIp = getLocalIpAddress()
                    if (myIp != null && myIp.contains(".")) {
                        val prefix = myIp.substringBeforeLast(".") + "."
                        val ipsToScan = (1..254).map { "$prefix$it" }

                        ipsToScan.chunked(25).forEach { batch ->
                            batch.forEach { targetIp ->
                                if (targetIp != myIp && targetIp != "127.0.0.1") {
                                    launch {
                                        runCatching {
                                            var isSweet = false
                                            var peerId = "usr_lan_${targetIp.replace(".", "_")}"
                                            var peerName = resolveDeviceModelName(targetIp)

                                            try {
                                                Socket().use { socket ->
                                                    socket.connect(InetSocketAddress(targetIp, HANDSHAKE_PORT), 150)
                                                    val writer = socket.getOutputStream().bufferedWriter()
                                                    val reader = socket.getInputStream().bufferedReader()
                                                    writer.write("SWEET_PING\n")
                                                    writer.flush()
                                                    val response = reader.readLine()
                                                    if (response?.startsWith("SWEET_PONG") == true) {
                                                        val parts = response.split("|")
                                                        peerId = parts.getOrNull(1) ?: peerId
                                                        peerName = parts.getOrNull(2) ?: peerName
                                                        isSweet = true
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                isSweet = false
                                            }

                                            val isReachable = if (isSweet) true else {
                                                runCatching { InetAddress.getByName(targetIp).isReachable(150) }.getOrDefault(false)
                                            }

                                            if (isReachable && peerId != myUserId) {
                                                val peer = NearbyPeer(
                                                    userId = peerId,
                                                    name = peerName,
                                                    host = targetIp,
                                                    port = HANDSHAKE_PORT,
                                                    rttMs = if (isSweet) 8L else 35L,
                                                    lastSeenTimestamp = System.currentTimeMillis(),
                                                    isSweetUser = isSweet
                                                )
                                                addOrUpdatePeer(peer, myUserId)
                                            }
                                        }
                                    }
                                }
                            }
                            delay(80)
                        }
                    }
                }
                delay(6000)
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

    private fun resolveService(service: NsdServiceInfo, myUserId: String) {
        @Suppress("DEPRECATION")
        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val userId = resolvedService.attributes["userId"]?.let { String(it) } ?: resolvedService.serviceName
                val name = resolvedService.attributes["displayName"]?.let { String(it) } ?: "SWEET Device"
                @Suppress("DEPRECATION")
                val host = resolvedService.host?.hostAddress ?: return
                val port = resolvedService.port

                val myIp = getLocalIpAddress()
                if (host != myIp && userId != myUserId) {
                    scope.launch {
                        val rtt = measureLatency(host, port)
                        val updatedPeer = NearbyPeer(
                            userId = userId,
                            name = name,
                            host = host,
                            port = port,
                            rttMs = rtt,
                            lastSeenTimestamp = System.currentTimeMillis(),
                            isSweetUser = true
                        )

                        addOrUpdatePeer(updatedPeer, myUserId)
                    }
                }
            }
        })
    }

    private suspend fun measureLatency(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1200)
                (System.currentTimeMillis() - start).coerceAtLeast(5L)
            }
        } catch (_: Exception) {
            18L
        }
    }

    private fun removePeer(userId: String) {
        _peers.value = _peers.value.filterNot { it.userId == userId }
    }

    override fun stopDiscovery() {
        runCatching {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
            registrationListener?.let { nsdManager.unregisterService(it) }
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
        heartbeatJob?.cancel()
        tcpServerJob?.cancel()
        subnetScannerJob?.cancel()
        cleanupJob?.cancel()
        _isDiscovering.value = false
    }
}
