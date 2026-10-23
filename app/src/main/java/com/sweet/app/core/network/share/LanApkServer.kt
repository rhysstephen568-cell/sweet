package com.sweet.app.core.network.share

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanApkServer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val SHARE_PORT = 9880
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareUrl = MutableStateFlow<String?>(null)
    val shareUrl: StateFlow<String?> = _shareUrl.asStateFlow()

    fun startServer(localIpAddress: String) {
        if (_isSharing.value) return

        val apkPath = context.applicationInfo.sourceDir
        val apkFile = File(apkPath)
        if (!apkFile.exists()) return

        _shareUrl.value = "http://$localIpAddress:$SHARE_PORT/sweet.apk"
        _isSharing.value = true

        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(SHARE_PORT))
                }

                while (isActive) {
                    val clientSocket = serverSocket.accept()
                    launch {
                        runCatching {
                            val output = clientSocket.getOutputStream()
                            val writer = output.bufferedWriter()

                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/vnd.android.package-archive\r\n" +
                                    "Content-Length: ${apkFile.length()}\r\n" +
                                    "Content-Disposition: attachment; filename=\"SWEET_v0.01.1.9.2.apk\"\r\n" +
                                    "Connection: close\r\n\r\n"

                            writer.write(header)
                            writer.flush()

                            val fileInput = FileInputStream(apkFile)
                            val buffer = ByteArray(64 * 1024)
                            var bytesRead: Int
                            while (fileInput.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                            output.flush()
                            fileInput.close()
                            clientSocket.close()
                        }
                    }
                }
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        _isSharing.value = false
        _shareUrl.value = null
    }
}
