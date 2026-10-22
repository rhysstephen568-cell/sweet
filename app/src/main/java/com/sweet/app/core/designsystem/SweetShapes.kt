package com.sweet.app.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class SweetShapes(
    val cornerPreset: CornerRadiusPreset,
    val bubbleIncomingShape: Shape,
    val bubbleOutgoingShape: Shape,
    val cardShape: Shape,
    val buttonShape: Shape
)

fun getSweetShapes(preset: CornerRadiusPreset): SweetShapes {
    val radius = when (preset) {
        CornerRadiusPreset.COMPACT -> 4.dp
        CornerRadiusPreset.DEFAULT -> 12.dp
        CornerRadiusPreset.ROUNDED -> 20.dp
    }
    val smallRadius = (radius.value / 3).dp

    return SweetShapes(
        cornerPreset = preset,
        bubbleIncomingShape = RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = smallRadius, bottomEnd = radius),
        bubbleOutgoingShape = RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = radius, bottomEnd = smallRadius),
        cardShape = RoundedCornerShape(radius),
        buttonShape = RoundedCornerShape(radius)
    )
}

val LocalSweetShapes = staticCompositionLocalOf {
    getSweetShapes(CornerRadiusPreset.DEFAULT)
}
