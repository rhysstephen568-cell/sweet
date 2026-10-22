package com.sweet.app.feature.settings.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sweet.app.core.datastore.DiscoveryScanType
import com.sweet.app.core.designsystem.SweetTheme
import com.sweet.app.core.designsystem.SweetTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverySettingsScreen(
    viewModel: DiscoveryViewModel,
    onBackClick: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = { Text("Discovery Settings") },
                onBackClick = onBackClick
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
                Text(
                    text = "Discovery Scan Visualization Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(DiscoveryScanType.entries.size) { index ->
                val type = DiscoveryScanType.entries[index]
                val isSelected = prefs.discoveryScanType == type

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectScanType(type) },
                    shape = SweetTheme.shapes.cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Radar, contentDescription = null)
                            Column {
                                Text(type.displayName, fontWeight = FontWeight.Bold)
                                Text(type.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Local Network Visibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
                                    selected = prefs.discoverableLocalNetwork == option,
                                    onClick = { viewModel.selectDiscoverableLocalNetwork(option) },
                                    label = { Text(option) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
