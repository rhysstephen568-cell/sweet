package com.sweet.app.feature.nearby

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val peers by viewModel.peers.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val wifiState by viewModel.wifiState.collectAsState()
    val selectedPeer by viewModel.selectedPeer.collectAsState()
    val isListView by viewModel.isListView.collectAsState()
    val blockedPeerIds by viewModel.blockedPeerIds.collectAsState()
    val shareUrl by viewModel.shareUrl.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val visiblePeers = remember(peers, blockedPeerIds) {
        peers.filterNot { it.userId in blockedPeerIds }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val extendedColors = SweetTheme.extended

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "SweepAngle"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2500, easing = FastOutSlowInEasing)),
        label = "PulseProgress"
    )

    // 5-second scale pulse animation for discovered non-SWEET LAN devices (2.5s scale up + 2.5s scale down = 5s cycle)
    val lanNodePulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LanNodePulseScale"
    )

    Scaffold(
        topBar = {
            SweetTopAppBar(
                title = {
                    Column {
                        Text(if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) "Radial Scan" else "360° Pulse Scan", fontWeight = FontWeight.Bold)
                        Text(
                            "Wi-Fi LAN Active: ${wifiState.ssid} ${if (wifiState.ipAddress != null) "(${wifiState.ipAddress})" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.onlineIndicator,
                            fontSize = 10.sp
                        )
                    }
                },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startShareApp()
                    showShareDialog = true
                },
                icon = { Icon(Icons.Default.Share, contentDescription = "Share App") },
                text = { Text("Share App") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = SweetTheme.shapes.buttonShape
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
                        Text("No nearby users or LAN devices found on this Wi-Fi network.\nCheck if Wi-Fi client isolation is enabled.", fontSize = 14.sp)
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
                                                .background(if (peer.isSweetUser) primaryColor else MaterialTheme.colorScheme.secondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(peer.name.take(1), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(peer.name, fontWeight = FontWeight.Bold)
                                                if (!peer.isSweetUser) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text("LAN Device", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("IP: ${peer.host} • ${peer.proximityTier.label}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (peer.isSweetUser) {
                                        Button(onClick = { onStartChat(peer.userId) }) {
                                            Text("Chat")
                                        }
                                    } else {
                                        OutlinedButton(onClick = {
                                            viewModel.startShareApp()
                                            showShareDialog = true
                                        }) {
                                            Text("Share App")
                                        }
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

                        drawCircle(
                            color = primaryColor,
                            radius = 20.dp.toPx(),
                            center = center
                        )

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

                            val baseRadius = 18.dp.toPx()
                            val nodeRadius = if (peer.isSweetUser) baseRadius else baseRadius * lanNodePulseScale
                            val nodeColor = if (peer.isSweetUser) extendedColors.onlineIndicator else Color(0xFFFFA000)

                            // 5-second pulsing outer aura glow ring for discovered non-SWEET LAN devices
                            if (!peer.isSweetUser) {
                                drawCircle(
                                    color = Color(0xFFFFA000).copy(alpha = (0.35f * (1.3f - (lanNodePulseScale - 0.85f))).coerceIn(0.05f, 0.45f)),
                                    radius = nodeRadius * 1.45f,
                                    center = nodePos
                                )
                            }

                            drawCircle(
                                color = nodeColor,
                                radius = nodeRadius,
                                center = nodePos
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        if (prefs.discoveryScanType == DiscoveryScanType.RADIAL) {
                            Text("• Mode: Radial Scan (Concentric Latency Tiers)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("• Green = SWEET Active • Orange Pulse = LAN Device", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("• Mode: 360° Pulse Scan (Expanding Wave Pulse)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("• Green = SWEET Active • Orange Pulse = LAN Device", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                .background(if (peer.isSweetUser) primaryColor else MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(peer.name.take(1), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(peer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("IP: ${peer.host} • Status: ${if (peer.isSweetUser) "SWEET Active" else "LAN Device (SWEET Not Installed)"}", style = MaterialTheme.typography.bodyMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (peer.isSweetUser) {
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
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.selectPeer(null)
                                        viewModel.startShareApp()
                                        showShareDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = SweetTheme.shapes.buttonShape
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share App over LAN")
                                }
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

            if (showShareDialog) {
                AlertDialog(
                    onDismissRequest = { showShareDialog = false },
                    title = { Text("Share SWEET App over LAN") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Any device on this Wi-Fi network (browser, phone, tablet, PC) can open this URL to download SWEET directly over LAN:")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = shareUrl ?: "http://${wifiState.ipAddress ?: "127.0.0.1"}:9880/sweet.apk",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, "Download SWEET App on Wi-Fi: ${shareUrl ?: ""}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Download Link"))
                            }
                        ) {
                            Text("Share Link")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showShareDialog = false
                                viewModel.stopShareApp()
                            }
                        ) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
