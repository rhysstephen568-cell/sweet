package com.sweet.app.core.network.beam

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class BeamTransferStatus {
    PENDING, TRANSFERRING, PAUSED, COMPLETED, FAILED
}

data class BeamFileTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val fileSize: Long,
    val bytesTransferred: Long = 0L,
    val speedMBs: Float = 0f,
    val progressPercent: Float = 0f,
    val status: BeamTransferStatus = BeamTransferStatus.PENDING,
    val checksumSha256: String = "",
    val isSender: Boolean = true
)

@Singleton
class SweetBeamManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val BEAM_PORT = 9879
    private val BUFFER_SIZE = 64 * 1024 // 64 KB

    private val _activeTasks = MutableStateFlow<Map<String, BeamFileTask>>(emptyMap())
    val activeTasks: StateFlow<Map<String, BeamFileTask>> = _activeTasks.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverJob: Job? = null

    init {
        startBeamTcpServer()
    }

    private fun startBeamTcpServer() {
        serverJob?.cancel()
        serverJob = scope.launch(Dispatchers.IO) {
            runCatching {
                val serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(BEAM_PORT))
                }

                while (isActive) {
                    val clientSocket = serverSocket.accept()
                    launch {
                        handleIncomingTransfer(clientSocket)
                    }
                }
            }
        }
    }

    private suspend fun handleIncomingTransfer(socket: Socket) = withContext(Dispatchers.IO) {
        runCatching {
            val input = socket.getInputStream()
            val reader = input.bufferedReader()
            val headerLine = reader.readLine() ?: return@withContext

            val parts = headerLine.split("|")
            if (parts.size >= 3 && parts[0] == "SWEET_BEAM") {
                val fileName = parts[1]
                val fileSize = parts[2].toLongOrNull() ?: 0L
                val fileId = java.util.UUID.randomUUID().toString()

                val downloadDir = File(context.getExternalFilesDir(null), "SweetBeam")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val outputFile = File(downloadDir, fileName)

                val task = BeamFileTask(
                    id = fileId,
                    fileName = fileName,
                    fileSize = fileSize,
                    status = BeamTransferStatus.TRANSFERRING,
                    isSender = false
                )
                updateTask(task)

                val digest = MessageDigest.getInstance("SHA-256")
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = 0L
                val startTime = System.currentTimeMillis()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000f
                    val speedMBs = if (elapsedTimeSec > 0) (totalBytesRead / (1024f * 1024f)) / elapsedTimeSec else 0f
                    val progress = if (fileSize > 0) (totalBytesRead.toFloat() / fileSize.toFloat()) * 100f else 0f

                    updateTask(
                        task.copy(
                            bytesTransferred = totalBytesRead,
                            speedMBs = speedMBs,
                            progressPercent = progress,
                            status = BeamTransferStatus.TRANSFERRING
                        )
                    )
                }

                outputStream.flush()
                outputStream.close()
                socket.close()

                val checksum = digest.digest().joinToString("") { "%02x".format(it) }
                updateTask(
                    task.copy(
                        bytesTransferred = totalBytesRead,
                        progressPercent = 100f,
                        status = BeamTransferStatus.COMPLETED,
                        checksumSha256 = checksum
                    )
                )
            }
        }
    }

    suspend fun sendFileP2P(targetHost: String, file: File): BeamFileTask = withContext(Dispatchers.IO) {
        val taskId = java.util.UUID.randomUUID().toString()
        val task = BeamFileTask(
            id = taskId,
            fileName = file.name,
            fileSize = file.length(),
            status = BeamTransferStatus.TRANSFERRING,
            isSender = true
        )
        updateTask(task)

        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetHost, BEAM_PORT), 3000)
                val outputStream = socket.getOutputStream()
                val writer = outputStream.bufferedWriter()

                // Send Header
                writer.write("SWEET_BEAM|${file.name}|${file.length()}\n")
                writer.flush()

                val fileInput = FileInputStream(file)
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesSent = 0L
                val startTime = System.currentTimeMillis()

                while (fileInput.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesSent += bytesRead

                    val elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000f
                    val speedMBs = if (elapsedTimeSec > 0) (totalBytesSent / (1024f * 1024f)) / elapsedTimeSec else 0f
                    val progress = if (file.length() > 0) (totalBytesSent.toFloat() / file.length().toFloat()) * 100f else 0f

                    updateTask(
                        task.copy(
                            bytesTransferred = totalBytesSent,
                            speedMBs = speedMBs,
                            progressPercent = progress,
                            status = BeamTransferStatus.TRANSFERRING
                        )
                    )
                }

                fileInput.close()
                outputStream.flush()

                val completedTask = task.copy(
                    bytesTransferred = totalBytesSent,
                    progressPercent = 100f,
                    status = BeamTransferStatus.COMPLETED
                )
                updateTask(completedTask)
                return@withContext completedTask
            }
        }

        val failedTask = task.copy(status = BeamTransferStatus.FAILED)
        updateTask(failedTask)
        return@withContext failedTask
    }

    private fun updateTask(task: BeamFileTask) {
        val currentMap = _activeTasks.value.toMutableMap()
        currentMap[task.id] = task
        _activeTasks.value = currentMap
    }
}
