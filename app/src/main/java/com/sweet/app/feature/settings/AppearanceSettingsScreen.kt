package com.sweet.app.feature.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sweet.app.core.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: ThemeViewModel,
    onBackClick: () -> Unit
) {
    val themeState by viewModel.themeState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance & Themes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Header Card
            item {
                Text(
                    text = "Live Theme Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Crossfade(
                    targetState = themeState.appearance,
                    animationSpec = tween(300),
                    label = "ThemePreviewCrossfade"
                ) { appearance ->
                    val previewColors = getAppearanceColors(
                        appearance = appearance,
                        catppuccinVariant = themeState.catppuccinVariant,
                        isDarkTheme = false,
                        context = context
                    )
                    val previewShapes = getSweetShapes(themeState.cornerRadiusPreset)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = previewShapes.cardShape,
                        colors = CardDefaults.cardColors(containerColor = previewColors.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Sample chat item header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(previewColors.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("S", color = previewColors.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SWEET Official",
                                        color = previewColors.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Welcome to SWEET messenger!",
                                        color = previewColors.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(previewColors.extendedColors.onlineIndicator)
                                )
                            }

                            // Incoming & Outgoing chat bubbles
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(previewShapes.bubbleIncomingShape)
                                    .background(previewColors.extendedColors.bubbleIncoming)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Hey! Check out this new live theme preview.",
                                    color = previewColors.extendedColors.bubbleIncomingText,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.End)
                                    .clip(previewShapes.bubbleOutgoingShape)
                                    .background(previewColors.extendedColors.bubbleOutgoing)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Looks super clean and smooth!",
                                    color = previewColors.extendedColors.bubbleOutgoingText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Options & Toggles
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Follow System Theme", fontWeight = FontWeight.Medium)
                            Switch(
                                checked = themeState.followSystem,
                                onCheckedChange = { viewModel.setFollowSystem(it) }
                            )
                        }

                        if (themeState.appearance == SweetAppearance.CATPPUCCIN) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text("Catppuccin Variant", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CatppuccinVariant.entries.forEach { variant ->
                                    FilterChip(
                                        selected = themeState.catppuccinVariant == variant,
                                        onClick = { viewModel.setCatppuccinVariant(variant) },
                                        label = { Text(variant.displayName) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Corner Radius Preset", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CornerRadiusPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = themeState.cornerRadiusPreset == preset,
                                    onClick = { viewModel.setCornerRadiusPreset(preset) },
                                    label = { Text(preset.displayName) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Text Size Scale: ${(themeState.fontScale * 100).toInt()}%", fontWeight = FontWeight.Medium)
                        Slider(
                            value = themeState.fontScale,
                            onValueChange = { viewModel.setFontScale(it) },
                            valueRange = 0.85f..1.30f,
                            steps = 3
                        )
                    }
                }
            }

            // Theme Selector List (15 appearances)
            item {
                Text(
                    text = "Select Appearance (15 Themes)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(SweetAppearance.entries) { appearance ->
                AppearanceCardItem(
                    appearance = appearance,
                    isSelected = themeState.appearance == appearance,
                    catppuccinVariant = themeState.catppuccinVariant,
                    cornerRadiusPreset = themeState.cornerRadiusPreset,
                    onClick = { viewModel.selectAppearance(appearance) }
                )
            }
        }
    }
}

@Composable
fun AppearanceCardItem(
    appearance: SweetAppearance,
    isSelected: Boolean,
    catppuccinVariant: CatppuccinVariant,
    cornerRadiusPreset: CornerRadiusPreset,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = getAppearanceColors(
        appearance = appearance,
        catppuccinVariant = catppuccinVariant,
        isDarkTheme = false,
        context = context
    )
    val shapes = getSweetShapes(cornerRadiusPreset)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shapes.cardShape)
                else Modifier
            ),
        shape = shapes.cardShape,
        colors = CardDefaults.cardColors(containerColor = colors.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = appearance.displayName,
                        color = colors.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (colors.isDark) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "DARK",
                                color = colors.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "LIGHT",
                                color = colors.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appearance.description,
                    color = colors.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = colors.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
