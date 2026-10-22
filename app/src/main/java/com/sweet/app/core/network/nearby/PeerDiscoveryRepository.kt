package com.sweet.app.core.network.nearby

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

interface PeerDiscoveryRepository {
    val peers: StateFlow<List<NearbyPeer>>
    val isDiscovering: StateFlow<Boolean>
    fun startDiscovery(currentUserId: String, displayName: String)
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
    private var cleanupJob: Job? = null

    private val SERVICE_TYPE = "_sweet._tcp."
    private val DISCOVERY_PORT = 9877

    override fun startDiscovery(currentUserId: String, displayName: String) {
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
            serviceName = "SWEET_$currentUserId"
            serviceType = SERVICE_TYPE
            port = 8088
            setAttribute("userId", currentUserId)
            setAttribute("displayName", displayName)
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
                if (service.serviceType.contains("_sweet") && !service.serviceName.contains(currentUserId)) {
                    resolveService(service)
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

        // Start UDP Heartbeat Broadcaster & Listener for instant subnet discovery
        startUdpHeartbeat(currentUserId, displayName)

        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                _peers.value = _peers.value.filter { now - it.lastSeenTimestamp < 20000 }
            }
        }
    }

    private fun startUdpHeartbeat(currentUserId: String, displayName: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            // Heartbeat Broadcaster
            launch {
                while (isActive) {
                    runCatching {
                        val socket = DatagramSocket()
                        socket.broadcast = true
                        val payload = "HEARTBEAT|$currentUserId|$displayName"
                        val bytes = payload.toByteArray()
                        val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
                        socket.send(packet)
                        socket.close()
                    }
                    delay(3000)
                }
            }

            // Heartbeat Listener
            launch {
                runCatching {
                    val socket = DatagramSocket(DISCOVERY_PORT)
                    val buffer = ByteArray(1024)
                    while (isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val receivedStr = String(packet.data, 0, packet.length)
                        val parts = receivedStr.split("|")
                        if (parts.size >= 3 && parts[0] == "HEARTBEAT") {
                            val userId = parts[1]
                            val name = parts[2]
                            val host = packet.address.hostAddress ?: "127.0.0.1"

                            if (userId != currentUserId) {
                                val newPeer = NearbyPeer(
                                    userId = userId,
                                    name = name,
                                    host = host,
                                    port = 8088,
                                    rttMs = 12L,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                                _peers.value = _peers.value.filterNot { it.userId == userId } + newPeer
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resolveService(service: NsdServiceInfo) {
        @Suppress("DEPRECATION")
        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                val userId = resolvedService.attributes["userId"]?.let { String(it) } ?: resolvedService.serviceName
                val name = resolvedService.attributes["displayName"]?.let { String(it) } ?: "Nearby User"
                @Suppress("DEPRECATION")
                val host = resolvedService.host?.hostAddress ?: return
                val port = resolvedService.port

                scope.launch {
                    val rtt = measureLatency(host, port)
                    val updatedPeer = NearbyPeer(
                        userId = userId,
                        name = name,
                        host = host,
                        port = port,
                        rttMs = rtt,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )

                    _peers.value = _peers.value.filterNot { it.userId == userId } + updatedPeer
                }
            }
        })
    }

    private suspend fun measureLatency(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1500)
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
        cleanupJob?.cancel()
        _isDiscovering.value = false
    }
}
