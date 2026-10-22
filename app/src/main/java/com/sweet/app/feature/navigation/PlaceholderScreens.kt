package com.sweet.app.feature.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
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
import com.sweet.app.core.designsystem.SweetTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPlaceholderScreen(onLoginSuccess: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SWEET v0.01.1.1") }) }
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
    onNavigateToSettings: () -> Unit,
    onNavigateToNearbyRadar: () -> Unit
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
                        Text("SWEET v0.01.1.1")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNearbyRadar) {
                        Icon(Icons.Default.Radar, contentDescription = "Nearby Radar")
                    }
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
            // Nearby Radar Banner Shortcut
            Card(
                onClick = onNavigateToNearbyRadar,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = SweetTheme.shapes.cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Radar, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text("Nearby Radial Chat Discovery", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Discover & chat with people on your Wi-Fi network", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Text(
                text = "Chats",
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
                            Text("Version 0.01.1.1 ready with Nearby Radar!", style = MaterialTheme.typography.bodyMedium)
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
            SweetTopAppBar(
                title = { Text("Chat #$chatId") },
                onBackClick = onBackClick
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
    onNavigateToProfile: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = { Text("Settings") },
                onBackClick = onBackClick
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
                onClick = onNavigateToProfile,
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
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                    Column {
                        Text("My Profile & Privacy", fontWeight = FontWeight.Bold)
                        Text("Name, username, bio, and Wi-Fi privacy settings", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

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
