package com.sweet.app.feature.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweet.app.R
import com.sweet.app.core.designsystem.SweetTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNearbyRadar: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()

    var editingAliasChatId by remember { mutableStateOf<String?>(null) }
    var aliasInputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                TopAppBar(
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "SWEET Logo",
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNearbyRadar,
                icon = { Icon(Icons.Default.Radar, contentDescription = "Find") },
                text = { Text("Find") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = SweetTheme.shapes.buttonShape
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active chats. Tap 'Find' to discover nearby users on Wi-Fi!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations, key = { it.chatId }) { conv ->
                        val timeStr = remember(conv.lastMessageTimestamp) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conv.lastMessageTimestamp))
                        }

                        Card(
                            onClick = {
                                viewModel.markChatAsRead(conv.chatId)
                                onNavigateToChat(conv.chatId)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = SweetTheme.shapes.cardShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_logo),
                                        contentDescription = "Contact Logo",
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = conv.displayName,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            if (conv.isPinned) {
                                                Icon(
                                                    Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        if (conv.isPeerTyping) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    "typing",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00E676)
                                                )
                                                TypingDotsIndicator(dotColor = Color(0xFF00E676))
                                            }
                                        } else {
                                            Text(
                                                text = conv.lastMessageContent,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                editingAliasChatId = conv.chatId
                                                aliasInputText = conv.customAlias ?: ""
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Name Alias", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { viewModel.togglePinConversation(conv.chatId) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                if (conv.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = "Pin Chat",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (conv.unreadCount > 0) {
                                            Badge { Text(conv.unreadCount.toString()) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Edit Personal Friend Alias Dialog
            editingAliasChatId?.let { targetChatId ->
                AlertDialog(
                    onDismissRequest = { editingAliasChatId = null },
                    title = { Text("Customize Friend Name") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Set a custom nickname for this friend. This only changes the name on your device, without overwriting their actual profile name:")
                            OutlinedTextField(
                                value = aliasInputText,
                                onValueChange = { aliasInputText = it },
                                placeholder = { Text("e.g. Dr. Smith (uLesson Doctor)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setCustomAlias(targetChatId, aliasInputText)
                                editingAliasChatId = null
                            }
                        ) {
                            Text("Save Name")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingAliasChatId = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
