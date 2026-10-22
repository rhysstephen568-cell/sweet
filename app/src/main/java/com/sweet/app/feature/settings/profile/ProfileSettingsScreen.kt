package com.sweet.app.feature.settings.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ProfileSettingsScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit,
    onNavigateToActiveSessions: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showUnsavedDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state.hasUnsavedChanges) {
        showUnsavedDialog = true
    }

    fun handleBack() {
        if (state.hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = { Text("Profile Settings") },
                onBackClick = { handleBack() },
                actions = {
                    if (state.hasUnsavedChanges) {
                        IconButton(onClick = { viewModel.saveProfile() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save Profile")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Profile Avatar",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.displayName,
                            onValueChange = { viewModel.onDisplayNameChanged(it) },
                            label = { Text("Display Name (1-50 chars)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            label = { Text("Username (@5-32 chars)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = !state.isUsernameAvailable,
                            supportingText = {
                                if (!state.isUsernameAvailable) {
                                    Text("Username unavailable or invalid", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )

                        OutlinedTextField(
                            value = state.bio,
                            onValueChange = { viewModel.onBioChanged(it) },
                            label = { Text("Bio (max 140 chars)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        OutlinedTextField(
                            value = state.phoneNumber,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Phone Number (Read-only)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Text("Privacy & Discovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SweetTheme.shapes.cardShape
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Discoverable on Local Wi-Fi Network", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Everyone", "Contacts", "Off").forEach { option ->
                                FilterChip(
                                    selected = state.discoverableLocalNetwork == option,
                                    onClick = { viewModel.onDiscoverableLocalNetworkChanged(option) },
                                    label = { Text(option) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Who can see my profile photo", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Everyone", "Contacts", "Nobody").forEach { option ->
                                FilterChip(
                                    selected = state.photoVisibility == option,
                                    onClick = { viewModel.onPhotoVisibilityChanged(option) },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SweetTheme.shapes.cardShape
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Active Sessions") },
                            supportingContent = { Text("Manage connected devices & revoke sessions") },
                            modifier = Modifier.clickable { onNavigateToActiveSessions() }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Blocked Users") },
                            supportingContent = { Text("View and manage blocked contacts") },
                            modifier = Modifier.clickable { onNavigateToBlockedUsers() }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Appearance & Themes") },
                            supportingContent = { Text("15 Jetpack Compose appearances") },
                            modifier = Modifier.clickable { onNavigateToAppearance() }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = SweetTheme.shapes.cardShape
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Danger Zone", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { /* Log out */ },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log Out", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }

        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("Discard Unsaved Changes?") },
                text = { Text("You have unsaved changes to your profile. Do you want to discard them?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            viewModel.discardChanges()
                            onBackClick()
                        }
                    ) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Keep Editing")
                    }
                }
            )
        }
    }
}
