package com.sweet.app.feature.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sweet.app.R
import com.sweet.app.core.designsystem.SweetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPlaceholderScreen(onLoginSuccess: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SWEET v0.01.1") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "SWEET App Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to SWEET",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Telegram-style instant messenger built with Jetpack Compose")
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = SweetTheme.shapes.buttonShape
            ) {
                Text("Continue with Phone Number")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListPlaceholderScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "SWEET Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Text("SWEET v0.01.1")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Chats Placeholder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                onClick = { onNavigateToChat("101") },
                modifier = Modifier.fillMaxWidth(),
                shape = SweetTheme.shapes.cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Contact Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column {
                            Text("SWEET Support Team", fontWeight = FontWeight.Bold)
                            Text("Version 0.01.1 ready on uLesson device!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Badge { Text("1") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationPlaceholderScreen(chatId: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat #$chatId") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Conversation Screen Placeholder for Chat $chatId")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaceholderScreen(
    onNavigateToAppearance: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = onNavigateToAppearance,
                modifier = Modifier.fillMaxWidth(),
                shape = SweetTheme.shapes.cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Appearance")
                    Column {
                        Text("Appearance & Themes", fontWeight = FontWeight.Bold)
                        Text("Explore all 15 appearances, colors, and presets", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
