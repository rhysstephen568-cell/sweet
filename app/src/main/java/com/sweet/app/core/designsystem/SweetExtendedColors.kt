package com.sweet.app.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SweetExtendedColors(
    val bubbleIncoming: Color,
    val bubbleOutgoing: Color,
    val bubbleIncomingText: Color,
    val bubbleOutgoingText: Color,
    val onlineIndicator: Color,
    val unreadBadge: Color,
    val unreadBadgeText: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val selectedNavContainer: Color
)

val LocalSweetExtendedColors = staticCompositionLocalOf {
    SweetExtendedColors(
        bubbleIncoming = Color(0xFFF0F0F0),
        bubbleOutgoing = Color(0xFF0088CC),
        bubbleIncomingText = Color(0xFF000000),
        bubbleOutgoingText = Color(0xFFFFFFFF),
        onlineIndicator = Color(0xFF4CAF50),
        unreadBadge = Color(0xFF0088CC),
        unreadBadgeText = Color(0xFFFFFFFF),
        divider = Color(0xFFE0E0E0),
        success = Color(0xFF2E7D32),
        warning = Color(0xFFED6C02),
        selectedNavContainer = Color(0xFFE0F2FE)
    )
}
