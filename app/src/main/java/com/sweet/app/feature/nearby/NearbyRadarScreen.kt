package com.sweet.app.feature.nearby

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweet.app.core.datastore.DiscoveryScanType
import com.sweet.app.core.designsystem.SweetTheme
import com.sweet.app.core.designsystem.SweetTopAppBar
import com.sweet.app.core.network.nearby.NearbyPeer
import com.sweet.app.core.network.nearby.ProximityTier
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyRadarScreen(
    viewModel: NearbyRadarViewModel,
    onBackClick: () -> Unit,
    onNavigateToDiscoverySettings: () -> Unit,
    onStartChat: (String) -> Unit
) {
    val peers by viewModel.peers.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val isListView by viewModel.isListView.collectAsState()
    val blockedPeerIds by viewModel.blockedPeerIds.collectAsState()

    val visiblePeers = remember(peers, blockedPeerIds) {
        peers.filterNot { it.userId in blockedPeerIds }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val extendedColors = SweetTheme.extended

    // Radar Sweep Animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "SweepAngle"
    )

    // 360 Pulse Wave Expansion Animation
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing)),
        label = "PulseProgress"
    )

    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = { Text(if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) "Radial Scan" else "360° Pulse Scan") },
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onNavigateToDiscoverySettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Discovery Settings")
                    }
                    IconButton(onClick = { viewModel.toggleListView() }) {
                        Icon(
                            imageVector = if (isListView) Icons.Default.Radar else Icons.AutoMirrored.Filled.List,
                            contentDescription = if (isListView) "Show Radar View" else "Show List View"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isListView) {
                if (visiblePeers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No nearby users found on this Wi-Fi network.\nCheck if Wi-Fi client isolation is enabled.", fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visiblePeers, key = { it.userId }) { peer ->
                            Card(
                                onClick = { viewModel.selectPeer(peer) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Nearby user ${peer.name}, latency ${peer.rttMs} ms" },
                                shape = SweetTheme.shapes.cardShape
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(peer.name.take(1), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text(peer.name, fontWeight = FontWeight.Bold)
                                            Text("${peer.proximityTier.label} (${peer.rttMs}ms)", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Button(onClick = { onStartChat(peer.userId) }) {
                                        Text("Chat")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(visiblePeers, prefs.discoveryScanType) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val maxRadius = minOf(size.width, size.height) * 0.42f

                                    visiblePeers.forEachIndexed { index, peer ->
                                        val (px, py) = if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) {
                                            val tierRatio = when (peer.proximityTier) {
                                                ProximityTier.VERY_CLOSE -> 0.25f
                                                ProximityTier.CLOSE -> 0.50f
                                                ProximityTier.FAR -> 0.75f
                                                ProximityTier.DISTANT -> 0.95f
                                            }
                                            val r = maxRadius * tierRatio
                                            val angleRad = peer.stableAngleRad
                                            Pair(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
                                        } else {
                                            // 360 Pulse Angle Spread
                                            val total = visiblePeers.size.coerceAtLeast(1)
                                            val step = 360f / total
                                            val angleDeg = index * step
                                            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                                            val r = maxRadius * 0.65f
                                            Pair(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
                                        }

                                        val dist = Offset(px, py).minus(offset).getDistance()
                                        if (dist < 50f) {
                                            viewModel.selectPeer(peer)
                                        }
                                    }
                                }
                            }
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = minOf(size.width, size.height) * 0.42f

                        if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) {
                            // Radial Scan Mode: Concentric Latency Rings
                            listOf(0.25f, 0.50f, 0.75f, 0.95f).forEach { ratio ->
                                drawCircle(
                                    color = outlineColor,
                                    radius = maxRadius * ratio,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            val sweepRad = Math.toRadians(sweepAngle.toDouble()).toFloat()
                            val lineEnd = Offset(
                                x = center.x + maxRadius * cos(sweepRad),
                                y = center.y + maxRadius * sin(sweepRad)
                            )
                            drawLine(
                                color = primaryColor.copy(alpha = 0.5f),
                                start = center,
                                end = lineEnd,
                                strokeWidth = 3.dp.toPx()
                            )
                        } else {
                            // 360° Pulse Scan Mode: Expanding Wave Pulse Circles
                            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { ratio ->
                                drawCircle(
                                    color = outlineColor,
                                    radius = maxRadius * ratio,
                                    center = center,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }

                            val expandingRadius = maxRadius * pulseProgress
                            val alpha = (1f - pulseProgress).coerceIn(0f, 1f)
                            drawCircle(
                                color = primaryColor.copy(alpha = alpha * 0.6f),
                                radius = expandingRadius,
                                center = center,
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }

                        // Center User Node
                        drawCircle(
                            color = primaryColor,
                            radius = 20.dp.toPx(),
                            center = center
                        )

                        // Discovered Peer Nodes in 360 Spectrum
                        visiblePeers.forEachIndexed { index, peer ->
                            val nodePos = if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) {
                                val tierRatio = when (peer.proximityTier) {
                                    ProximityTier.VERY_CLOSE -> 0.25f
                                    ProximityTier.CLOSE -> 0.50f
                                    ProximityTier.FAR -> 0.75f
                                    ProximityTier.DISTANT -> 0.95f
                                }
                                val r = maxRadius * tierRatio
                                val angleRad = peer.stableAngleRad
                                Offset(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
                            } else {
                                val total = visiblePeers.size.coerceAtLeast(1)
                                val step = 360f / total
                                val angleDeg = index * step
                                val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                                val r = maxRadius * 0.65f
                                Offset(center.x + r * cos(angleRad), center.y + r * sin(angleRad))
                            }

                            drawCircle(
                                color = extendedColors.onlineIndicator,
                                radius = 18.dp.toPx(),
                                center = nodePos
                            )
                        }
                    }

                    // Mode Mode Footer Legend
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) {
                            Text("• Mode: Radial Scan (Concentric Latency Tiers)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("• Ring 1: Very Close (<15ms) • Ring 2: Close (<40ms)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("• Mode: 360° Pulse Scan (Expanding Wave Pulse)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("• 360-degree radar spectrum wave discovery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            selectedPeer?.let { peer ->
                ModalBottomSheet(
                    onDismissRequest = { viewModel.selectPeer(null) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(peer.name.take(1), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(peer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("IP: ${peer.host} • Latency: ${peer.rttMs} ms", style = MaterialTheme.typography.bodyMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.selectPeer(null)
                                    onStartChat(peer.userId)
                                },
                                modifier = Modifier.weight(1f),
                                shape = SweetTheme.shapes.buttonShape
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Chat")
                            }

                            OutlinedButton(
                                onClick = { viewModel.blockPeer(peer.userId) },
                                modifier = Modifier.weight(1f),
                                shape = SweetTheme.shapes.buttonShape
                            ) {
                                Text("Block Node")
                            }
                        }
                    }
                }
            }
        }
    }
}
