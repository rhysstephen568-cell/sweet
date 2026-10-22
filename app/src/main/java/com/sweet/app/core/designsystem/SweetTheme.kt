package com.sweet.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

@Composable
fun SweetTheme(
    appearance: SweetAppearance = SweetAppearance.SWEET_LIGHT,
    catppuccinVariant: CatppuccinVariant = CatppuccinVariant.MOCHA,
    cornerRadiusPreset: CornerRadiusPreset = CornerRadiusPreset.DEFAULT,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appearanceColors = getAppearanceColors(
        appearance = appearance,
        catppuccinVariant = catppuccinVariant,
        isDarkTheme = isDarkTheme,
        context = context
    )
    val shapes = getSweetShapes(cornerRadiusPreset)

    CompositionLocalProvider(
        LocalSweetExtendedColors provides appearanceColors.extendedColors,
        LocalSweetShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = appearanceColors.colorScheme,
            content = content
        )
    }
}

object SweetTheme {
    val extended: SweetExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSweetExtendedColors.current

    val shapes: SweetShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalSweetShapes.current
}
