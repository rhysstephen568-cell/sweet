package com.sweet.app.core.network.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class WifiStateInfo(
    val isConnected: Boolean = false,
    val ssid: String = "Wi-Fi Subnet",
    val rssi: Int = -50,
    val signalLevel: Int = 4,
    val ipAddress: String? = null
)

interface WifiNetworkMonitor {
    val wifiState: StateFlow<WifiStateInfo>
    fun startMonitoring()
    fun stopMonitoring()
}

@Singleton
class WifiNetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WifiNetworkMonitor {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _wifiState = MutableStateFlow(WifiStateInfo())
    override val wifiState: StateFlow<WifiStateInfo> = _wifiState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun startMonitoring() {
        if (networkCallback != null) return

        updateWifiState()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateWifiState()
            }

            override fun onLost(network: Network) {
                _wifiState.value = WifiStateInfo(isConnected = false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateWifiState()
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        }
    }

    private fun updateWifiState() {
        runCatching {
            @Suppress("DEPRECATION")
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo
            @Suppress("DEPRECATION")
            val isConnected = wifiInfo != null && wifiInfo.ipAddress != 0
            val rawSsid = wifiInfo?.ssid?.replace("\"", "") ?: "Wi-Fi Subnet"
            val rssi = wifiInfo?.rssi ?: -50
            @Suppress("DEPRECATION")
            val level = WifiManager.calculateSignalLevel(rssi, 5)

            @Suppress("DEPRECATION")
            val ipInt = wifiInfo?.ipAddress ?: 0
            val ipStr = if (ipInt != 0) {
                String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else null

            _wifiState.value = WifiStateInfo(
                isConnected = isConnected,
                ssid = if (rawSsid == "<unknown ssid>") "Wi-Fi Subnet" else rawSsid,
                rssi = rssi,
                signalLevel = level,
                ipAddress = ipStr
            )
        }
    }

    override fun stopMonitoring() {
        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
            networkCallback = null
        }
    }
}
