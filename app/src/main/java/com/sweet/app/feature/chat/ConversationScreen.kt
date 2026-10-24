package com.sweet.app.feature.chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweet.app.R
import com.sweet.app.core.designsystem.SweetTheme
import com.sweet.app.core.designsystem.SweetTopAppBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConversationScreen(
    viewModel: ChatViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val isSecretMode by viewModel.isSecretMode.collectAsState()
    val selfDestructSeconds by viewModel.selfDestructSeconds.collectAsState()
    val extendedColors = SweetTheme.extended

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showTimerMenu by remember { mutableStateOf(false) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = resolveFileName(context, uri)
            val sizeStr = resolveFileSize(context, uri)
            viewModel.sendMessage(
                fileName = name,
                fileSizeStr = sizeStr,
                isBeamFile = true
            )
        }
    }

    val activity = context as? Activity
    DisposableEffect(isSecretMode) {
        if (isSecretMode && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { /* TODO: Open Peer Profile */ },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isSecretMode) {
                                    Icon(Icons.Default.Lock, contentDescription = "Secret Chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = if (isSecretMode) "Secret Chat" else viewModel.peerDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isPeerTyping) {
                                    Text("typing", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                                    TypingDotsIndicator(dotColor = Color(0xFF00E676))
                                } else {
                                    Text(if (isSecretMode) "E2EE Hardware Keystore Active" else "Online on Wi-Fi Subnet", style = MaterialTheme.typography.bodySmall, color = extendedColors.onlineIndicator)
                                }
                            }
                        }
                    }
                },
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.toggleSecretMode() }) {
                        Icon(
                            imageVector = if (isSecretMode) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Secret Chat Mode",
                            tint = if (isSecretMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSecretMode) {
                        Box {
                            IconButton(onClick = { showTimerMenu = true }) {
                                Icon(Icons.Default.Timer, contentDescription = "Self-Destruct Timer")
                            }
                            DropdownMenu(
                                expanded = showTimerMenu,
                                onDismissRequest = { showTimerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Self-Destruct: Off") },
                                    onClick = {
                                        viewModel.setSelfDestructSeconds(0)
                                        showTimerMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Self-Destruct: 5 Seconds") },
                                    onClick = {
                                        viewModel.setSelfDestructSeconds(5)
                                        showTimerMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Self-Destruct: 10 Seconds") },
                                    onClick = {
                                        viewModel.setSelfDestructSeconds(10)
                                        showTimerMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Self-Destruct: 1 Minute") },
                                    onClick = {
                                        viewModel.setSelfDestructSeconds(60)
                                        showTimerMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    replyingToMessage?.let { replyTarget ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("Replying to ${replyTarget.senderName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(replyTarget.content, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = { showAttachmentSheet = true }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attachments & Beam P2P")
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            placeholder = { Text(if (isSecretMode) "Secret E2EE message..." else "Write a message...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            shape = SweetTheme.shapes.buttonShape
                        )

                        IconButton(
                            onClick = {
                                viewModel.sendMessage(replyToMsg = replyingToMessage)
                                replyingToMessage = null
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSecretMode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            shape = SweetTheme.shapes.cardShape
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Secret Chat Activated", fontWeight = FontWeight.Bold)
                                }
                                Text("• On-device E2EE with Android Keystore AES-256", fontSize = 11.sp)
                                Text("• Screen recording & screenshots blocked (FLAG_SECURE)", fontSize = 11.sp)
                                Text("• Self-destruct timer: ${if (selfDestructSeconds > 0) "$selfDestructSeconds sec" else "Off"}", fontSize = 11.sp)
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    MessageBubbleItem(
                        message = msg,
                        extendedColors = extendedColors,
                        onSelfDestruct = { viewModel.deleteMessage(msg.id) },
                        onSelectMessage = { selectedMessageForActions = msg }
                    )
                }

                if (isPeerTyping) {
                    item {
                        Surface(
                            shape = SweetTheme.shapes.bubbleIncomingShape,
                            color = extendedColors.bubbleIncoming,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("typing", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                TypingDotsIndicator(dotColor = Color(0xFF00E676))
                            }
                        }
                    }
                }
            }

            selectedMessageForActions?.let { msg ->
                ModalBottomSheet(
                    onDismissRequest = { selectedMessageForActions = null }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Message Reactions & Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clickable {
                                            viewModel.addReaction(msg.id, emoji)
                                            selectedMessageForActions = null
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Reply") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                            modifier = Modifier.clickable {
                                replyingToMessage = msg
                                selectedMessageForActions = null
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Copy Text") },
                            leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SWEET Message", msg.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                                selectedMessageForActions = null
                            }
                        )

                        ListItem(
                            headlineContent = { Text(if (msg.isStarred) "Unstar Message" else "Star Message") },
                            leadingContent = { Icon(if (msg.isStarred) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.toggleStarMessage(msg.id)
                                selectedMessageForActions = null
                            }
                        )

                        ListItem(
                            headlineContent = { Text("Delete Message", color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                viewModel.deleteMessage(msg.id)
                                selectedMessageForActions = null
                            }
                        )
                    }
                }
            }

            if (showAttachmentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAttachmentSheet = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("SWEET Attachments & P2P Beam", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        ListItem(
                            headlineContent = { Text("Select Real Document or Media") },
                            supportingContent = { Text("Choose any real file from your device library") },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                showAttachmentSheet = false
                                filePickerLauncher.launch("*/*")
                            }
                        )

                        HorizontalDivider()

                        ListItem(
                            headlineContent = { Text("Photo or Image") },
                            leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showAttachmentSheet = false
                                filePickerLauncher.launch("image/*")
                            }
                        )

                        ListItem(
                            headlineContent = { Text("PDF or Document") },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null) },
                            modifier = Modifier.clickable {
                                showAttachmentSheet = false
                                filePickerLauncher.launch("application/pdf")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    extendedColors: com.sweet.app.core.designsystem.SweetExtendedColors,
    onSelfDestruct: () -> Unit,
    onSelectMessage: () -> Unit
) {
    val isFromMe = message.isFromMe
    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    var remainingSeconds by remember { mutableStateOf(message.selfDestructSeconds) }

    LaunchedEffect(message.selfDestructSeconds) {
        if (message.selfDestructSeconds > 0) {
            while (remainingSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                remainingSeconds--
            }
            onSelfDestruct()
        }
    }

    val bubbleShape = if (isFromMe) SweetTheme.shapes.bubbleOutgoingShape else SweetTheme.shapes.bubbleIncomingShape
    val bubbleColor = if (isFromMe) extendedColors.bubbleOutgoing else extendedColors.bubbleIncoming
    val textColor = if (isFromMe) extendedColors.bubbleOutgoingText else extendedColors.bubbleIncomingText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectMessage() },
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isFromMe) {
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (message.replyToContent != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = textColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(message.replyToSenderName ?: "Reply", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = textColor)
                            Text(message.replyToContent, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = textColor)
                        }
                    }
                }

                if (message.isBeamFile) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = "SWEET Beam", tint = textColor)
                        Column {
                            Text(message.fileName ?: "File Transfer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${message.fileSizeStr ?: "Unknown Size"} • P2P Direct", fontSize = 10.sp, color = textColor.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { message.transferProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = textColor,
                    )
                } else {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isStarred) {
                        Icon(Icons.Default.Star, contentDescription = "Starred", tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                    }

                    if (message.selfDestructSeconds > 0) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Burn Timer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                        Text("${remainingSeconds}s", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    if (isFromMe) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp)
            ) {
                message.reactions.values.distinct().forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Text(emoji, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

private fun resolveFileName(context: Context, uri: Uri): String {
    return runCatching {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        result ?: uri.path?.substringAfterLast('/') ?: "shared_document.pdf"
    }.getOrDefault("shared_file.pdf")
}

private fun resolveFileSize(context: Context, uri: Uri): String {
    return runCatching {
        var sizeBytes: Long = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) sizeBytes = cursor.getLong(index)
                }
            }
        }
        val mb = sizeBytes / (1024f * 1024f)
        if (mb >= 0.1f) String.format(Locale.US, "%.1f MB", mb) else "${sizeBytes / 1024} KB"
    }.getOrDefault("Unknown Size")
}
