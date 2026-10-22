package com.sweet.app.core.network.share

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sweet.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

data class LanShareRequest(
    val senderName: String,
    val shareUrl: String
)

@Singleton
class LanShareNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val CHANNEL_ID = "sweet_lan_share_channel"
    private val NOTIF_PORT = 9877
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listenerJob: Job? = null

    private val _incomingShareRequest = MutableStateFlow<LanShareRequest?>(null)
    val incomingShareRequest: StateFlow<LanShareRequest?> = _incomingShareRequest.asStateFlow()

    init {
        createNotificationChannel()
        startLanShareListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LAN App Share Downloads",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority pop-up interrupt notification for LAN app share downloads"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun broadcastLanShareSignal(shareUrl: String, senderName: String) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket()
                socket.broadcast = true
                val payload = "SWEET_LAN_SHARE_NOTIF|$shareUrl|$senderName"
                val bytes = payload.toByteArray()

                val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), NOTIF_PORT)
                socket.send(packet)

                val emuPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName("10.0.2.2"), NOTIF_PORT)
                socket.send(emuPacket)

                socket.close()
            }
        }
    }

    private fun startLanShareListener() {
        listenerJob?.cancel()
        listenerJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(NOTIF_PORT))
                }
                val buffer = ByteArray(2048)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val receivedStr = String(packet.data, 0, packet.length)
                    val parts = receivedStr.split("|")

                    if (parts.size >= 3 && parts[0] == "SWEET_LAN_SHARE_NOTIF") {
                        val shareUrl = parts[1]
                        val senderName = parts[2]

                        _incomingShareRequest.value = LanShareRequest(senderName, shareUrl)
                        showHeadsUpNotification(senderName, shareUrl)
                    }
                }
            }
        }
    }

    fun dismissRequest() {
        _incomingShareRequest.value = null
    }

    private fun showHeadsUpNotification(senderName: String, shareUrl: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl)).apply {
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
                .setContentTitle("Incoming SWEET App Share")
                .setContentText("$senderName is sharing SWEET App! Tap to download over LAN.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(1001, builder.build())
                }
            } else {
                notificationManager.notify(1001, builder.build())
            }
        }
    }
}
