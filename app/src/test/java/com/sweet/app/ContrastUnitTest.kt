package com.sweet.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sweet.app.core.designsystem.CatppuccinVariant
import com.sweet.app.core.designsystem.SweetAppearance
import com.sweet.app.core.designsystem.getAppearanceColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ContrastUnitTest {

    private fun calculateRelativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0

        fun transform(c: Double): Double {
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }

        val lr = transform(r)
        val lg = transform(g)
        val lb = transform(b)

        return 0.2126 * lr + 0.7152 * lg + 0.0722 * lb
    }

    private fun calculateContrastRatio(fg: Color, bg: Color): Double {
        val l1 = calculateRelativeLuminance(fg)
        val l2 = calculateRelativeLuminance(bg)
        val brighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (brighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testAllAppearancesBodyTextContrast() {
        SweetAppearance.entries.forEach { appearance ->
            val appearanceColors = getAppearanceColors(
                appearance = appearance,
                catppuccinVariant = CatppuccinVariant.MOCHA,
                isDarkTheme = appearance.name.contains("DARK") || appearance == SweetAppearance.AMOLED_BLACK
            )

            val colorScheme = appearanceColors.colorScheme
            val ratioOnBackground = calculateContrastRatio(colorScheme.onBackground, colorScheme.background)
            val ratioOnSurface = calculateContrastRatio(colorScheme.onSurface, colorScheme.surface)

            val requiredRatio = if (appearance == SweetAppearance.HIGH_CONTRAST) 7.0 else 4.5

            assertTrue(
                "Appearance ${appearance.displayName} onBackground contrast ratio $ratioOnBackground < $requiredRatio",
                ratioOnBackground >= requiredRatio
            )

            assertTrue(
                "Appearance ${appearance.displayName} onSurface contrast ratio $ratioOnSurface < $requiredRatio",
                ratioOnSurface >= requiredRatio
            )
        }
    }
}
