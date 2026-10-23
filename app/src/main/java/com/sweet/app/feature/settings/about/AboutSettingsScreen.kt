package com.sweet.app.feature.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweet.app.R
import com.sweet.app.core.designsystem.SweetTheme
import com.sweet.app.core.designsystem.SweetTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = { Text("About SWEET") },
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "SWEET App Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                )
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SWEET Messenger",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 0.01.1.7.6 (Build 15)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SweetTheme.shapes.cardShape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Architecture & Core Capabilities", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("• Kotlin 2.0, Jetpack Compose Material 3, Clean MVVM, Hilt DI")
                        Text("• 15 WCAG-compliant M3 Themes & Catppuccin Variants")
                        Text("• Radial & 360° Pulse Scan Radar with 5s LAN scale pulse animation")
                        Text("• Wi-Fi Network Monitor & Subnet Scanner for all LAN devices")
                        Text("• SWEET Beam: Offline P2P chunked file streaming over TCP port 9879")
                        Text("• In-App HTTP APK Share Server over LAN with high-priority notifications")
                        Text("• E2EE Secret Chats: Android Keystore AES-256 & FLAG_SECURE Anti-Screenshot")
                        Text("• Self-Destructing Messages with burn countdown timers")
                        Text("• Real-time local Wi-Fi messaging & animated typing indicators")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SweetTheme.shapes.cardShape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Target Build Environment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("• Min SDK: 26 (Android 8.0 Oreo)")
                        Text("• Target SDK: 30 / Compile SDK: 34")
                        Text("• AGP 8.5.2 • Gradle 8.14 • JDK 17")
                    }
                }
            }

            item {
                Text(
                    text = "© 2026 SWEET Inc. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
