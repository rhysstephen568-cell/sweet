package com.sweet.app.core.designsystem

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

data class AppearanceColors(
    val colorScheme: ColorScheme,
    val extendedColors: SweetExtendedColors,
    val isDark: Boolean
)

fun getAppearanceColors(
    appearance: SweetAppearance,
    catppuccinVariant: CatppuccinVariant,
    isDarkTheme: Boolean,
    context: Context? = null
): AppearanceColors {
    return when (appearance) {
        SweetAppearance.SWEET_LIGHT -> AppearanceColors(
            colorScheme = lightColorScheme(
                primary = Color(0xFF0088CC),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE0F2FE),
                onPrimaryContainer = Color(0xFF001F3F),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF1C1B1F),
                surfaceVariant = Color(0xFFF4F4F5),
                onSurfaceVariant = Color(0xFF44474E),
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                outline = Color(0xFFCBD5E1)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFFFFFFFF),
                bubbleOutgoing = Color(0xFFE0F2FE),
                bubbleIncomingText = Color(0xFF0F172A),
                bubbleOutgoingText = Color(0xFF003366),
                onlineIndicator = Color(0xFF22C55E),
                unreadBadge = Color(0xFF0088CC),
                unreadBadgeText = Color(0xFFFFFFFF),
                divider = Color(0xFFE2E8F0),
                success = Color(0xFF16A34A),
                warning = Color(0xFFD97706),
                selectedNavContainer = Color(0xFFBAE6FD)
            ),
            isDark = false
        )

        SweetAppearance.SWEET_DARK -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF003366),
                primaryContainer = Color(0xFF004D80),
                onPrimaryContainer = Color(0xFFE0F2FE),
                surface = Color(0xFF1E293B),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF334155),
                onSurfaceVariant = Color(0xFFCBD5E1),
                background = Color(0xFF0F172A),
                onBackground = Color(0xFFF8FAFC),
                outline = Color(0xFF475569)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF334155),
                bubbleOutgoing = Color(0xFF0284C7),
                bubbleIncomingText = Color(0xFFF8FAFC),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF4ADE80),
                unreadBadge = Color(0xFF38BDF8),
                unreadBadgeText = Color(0xFF0F172A),
                divider = Color(0xFF334155),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                selectedNavContainer = Color(0xFF0369A1)
            ),
            isDark = true
        )

        SweetAppearance.AMOLED_BLACK -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF00B0FF),
                onPrimary = Color(0xFF000000),
                primaryContainer = Color(0xFF121212),
                onPrimaryContainer = Color(0xFF80D8FF),
                surface = Color(0xFF121212),
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF1E1E1E),
                onSurfaceVariant = Color(0xFFE0E0E0),
                background = Color(0xFF000000),
                onBackground = Color(0xFFFFFFFF),
                outline = Color(0xFF333333)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF181818),
                bubbleOutgoing = Color(0xFF005588),
                bubbleIncomingText = Color(0xFFFFFFFF),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF00E676),
                unreadBadge = Color(0xFF00B0FF),
                unreadBadgeText = Color(0xFF000000),
                divider = Color(0xFF222222),
                success = Color(0xFF00E676),
                warning = Color(0xFFFFAB00),
                selectedNavContainer = Color(0xFF003355)
            ),
            isDark = true
        )

        SweetAppearance.MATERIAL_YOU -> {
            val isDark = isDarkTheme
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFD0BCFF),
                        onPrimary = Color(0xFF381E72),
                        surface = Color(0xFF1C1B1F),
                        onSurface = Color(0xFFE6E1E5),
                        background = Color(0xFF141218),
                        onBackground = Color(0xFFE6E1E5)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF6750A4),
                        onPrimary = Color(0xFFFFFFFF),
                        surface = Color(0xFFFEF7FF),
                        onSurface = Color(0xFF1D1B20),
                        background = Color(0xFFFEF7FF),
                        onBackground = Color(0xFF1D1B20)
                    )
                }
            }
            AppearanceColors(
                colorScheme = colorScheme,
                extendedColors = SweetExtendedColors(
                    bubbleIncoming = colorScheme.surfaceVariant,
                    bubbleOutgoing = colorScheme.primaryContainer,
                    bubbleIncomingText = colorScheme.onSurfaceVariant,
                    bubbleOutgoingText = colorScheme.onPrimaryContainer,
                    onlineIndicator = Color(0xFF4CAF50),
                    unreadBadge = colorScheme.primary,
                    unreadBadgeText = colorScheme.onPrimary,
                    divider = colorScheme.outline,
                    success = Color(0xFF2E7D32),
                    warning = Color(0xFFED6C02),
                    selectedNavContainer = colorScheme.secondaryContainer
                ),
                isDark = isDark
            )
        }

        SweetAppearance.MIDNIGHT_BLUE -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF00D2FF),
                onPrimary = Color(0xFF002244),
                surface = Color(0xFF0F172A),
                onSurface = Color(0xFFF1F5F9),
                surfaceVariant = Color(0xFF1E293B),
                onSurfaceVariant = Color(0xFF94A3B8),
                background = Color(0xFF0B1120),
                onBackground = Color(0xFFF1F5F9),
                outline = Color(0xFF334155)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF1E293B),
                bubbleOutgoing = Color(0xFF00529B),
                bubbleIncomingText = Color(0xFFF1F5F9),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF00E5FF),
                unreadBadge = Color(0xFF00D2FF),
                unreadBadgeText = Color(0xFF002244),
                divider = Color(0xFF1E293B),
                success = Color(0xFF10B981),
                warning = Color(0xFFF59E0B),
                selectedNavContainer = Color(0xFF0284C7)
            ),
            isDark = true
        )

        SweetAppearance.TOKYO_NIGHT -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF7AA2F7),
                onPrimary = Color(0xFF1A1B26),
                surface = Color(0xFF24283B),
                onSurface = Color(0xFFC0CAF5),
                surfaceVariant = Color(0xFF2F354F),
                onSurfaceVariant = Color(0xFFA9B1D6),
                background = Color(0xFF1A1B26),
                onBackground = Color(0xFFC0CAF5),
                outline = Color(0xFF414868)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF2F354F),
                bubbleOutgoing = Color(0xFF3B4261),
                bubbleIncomingText = Color(0xFFC0CAF5),
                bubbleOutgoingText = Color(0xFF7DCFFF),
                onlineIndicator = Color(0xFF9ECE6A),
                unreadBadge = Color(0xFFBB9AF7),
                unreadBadgeText = Color(0xFF1A1B26),
                divider = Color(0xFF292E42),
                success = Color(0xFF9ECE6A),
                warning = Color(0xFFE0AF68),
                selectedNavContainer = Color(0xFF3B4261)
            ),
            isDark = true
        )

        SweetAppearance.NORD -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF88C0D0),
                onPrimary = Color(0xFF2E3440),
                surface = Color(0xFF3B4252),
                onSurface = Color(0xFFECEFF4),
                surfaceVariant = Color(0xFF434C5E),
                onSurfaceVariant = Color(0xFFD8DEE9),
                background = Color(0xFF2E3440),
                onBackground = Color(0xFFECEFF4),
                outline = Color(0xFF4C566A)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF434C5E),
                bubbleOutgoing = Color(0xFF5E81AC),
                bubbleIncomingText = Color(0xFFECEFF4),
                bubbleOutgoingText = Color(0xFFECEFF4),
                onlineIndicator = Color(0xFFA3BE8C),
                unreadBadge = Color(0xFF81A1C1),
                unreadBadgeText = Color(0xFF2E3440),
                divider = Color(0xFF4C566A),
                success = Color(0xFFA3BE8C),
                warning = Color(0xFFEBCB8B),
                selectedNavContainer = Color(0xFF434C5E)
            ),
            isDark = true
        )

        SweetAppearance.CATPPUCCIN -> getCatppuccinColors(catppuccinVariant)

        SweetAppearance.FOREST -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF52B788),
                onPrimary = Color(0xFF081C15),
                surface = Color(0xFF1B4332),
                onSurface = Color(0xFFD8F3DC),
                surfaceVariant = Color(0xFF2D6A4F),
                onSurfaceVariant = Color(0xFFB7E4C7),
                background = Color(0xFF081C15),
                onBackground = Color(0xFFD8F3DC),
                outline = Color(0xFF40916C)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF1B4332),
                bubbleOutgoing = Color(0xFF2D6A4F),
                bubbleIncomingText = Color(0xFFD8F3DC),
                bubbleOutgoingText = Color(0xFFD8F3DC),
                onlineIndicator = Color(0xFF74C69D),
                unreadBadge = Color(0xFF52B788),
                unreadBadgeText = Color(0xFF081C15),
                divider = Color(0xFF1B4332),
                success = Color(0xFF52B788),
                warning = Color(0xFFE9C46A),
                selectedNavContainer = Color(0xFF2D6A4F)
            ),
            isDark = true
        )

        SweetAppearance.SUNSET -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFFF7B00),
                onPrimary = Color(0xFF331000),
                surface = Color(0xFF2B1E1A),
                onSurface = Color(0xFFFFEBD9),
                surfaceVariant = Color(0xFF3D2A24),
                onSurfaceVariant = Color(0xFFFFC299),
                background = Color(0xFF1A120F),
                onBackground = Color(0xFFFFEBD9),
                outline = Color(0xFF664438)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF3D2A24),
                bubbleOutgoing = Color(0xFFD04A02),
                bubbleIncomingText = Color(0xFFFFEBD9),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFFFFB703),
                unreadBadge = Color(0xFFFF7B00),
                unreadBadgeText = Color(0xFF331000),
                divider = Color(0xFF3D2A24),
                success = Color(0xFF38B000),
                warning = Color(0xFFFFB703),
                selectedNavContainer = Color(0xFF662200)
            ),
            isDark = true
        )

        SweetAppearance.ROSE -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFF472B6),
                onPrimary = Color(0xFF500724),
                surface = Color(0xFF2A1B24),
                onSurface = Color(0xFFFDF2F8),
                surfaceVariant = Color(0xFF3B2533),
                onSurfaceVariant = Color(0xFFFBCFE8),
                background = Color(0xFF1C1017),
                onBackground = Color(0xFFFDF2F8),
                outline = Color(0xFF703254)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF3B2533),
                bubbleOutgoing = Color(0xFF9D174D),
                bubbleIncomingText = Color(0xFFFDF2F8),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF34D399),
                unreadBadge = Color(0xFFF472B6),
                unreadBadgeText = Color(0xFF500724),
                divider = Color(0xFF3B2533),
                success = Color(0xFF10B981),
                warning = Color(0xFFF59E0B),
                selectedNavContainer = Color(0xFF831843)
            ),
            isDark = true
        )

        SweetAppearance.MONOCHROME -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFFFFFFF),
                onPrimary = Color(0xFF000000),
                surface = Color(0xFF181818),
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF282828),
                onSurfaceVariant = Color(0xFFCCCCCC),
                background = Color(0xFF000000),
                onBackground = Color(0xFFFFFFFF),
                outline = Color(0xFF666666)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF242424),
                bubbleOutgoing = Color(0xFF404040),
                bubbleIncomingText = Color(0xFFFFFFFF),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFFFFFFFF),
                unreadBadge = Color(0xFFFFFFFF),
                unreadBadgeText = Color(0xFF000000),
                divider = Color(0xFF333333),
                success = Color(0xFFCCCCCC),
                warning = Color(0xFF888888),
                selectedNavContainer = Color(0xFF333333)
            ),
            isDark = true
        )

        SweetAppearance.HIGH_CONTRAST -> AppearanceColors(
            colorScheme = lightColorScheme(
                primary = Color(0xFF000000),
                onPrimary = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF000000),
                surfaceVariant = Color(0xFFEEEEEE),
                onSurfaceVariant = Color(0xFF000000),
                background = Color(0xFFFFFFFF),
                onBackground = Color(0xFF000000),
                outline = Color(0xFF000000)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFFF0F0F0),
                bubbleOutgoing = Color(0xFF000000),
                bubbleIncomingText = Color(0xFF000000),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF008000),
                unreadBadge = Color(0xFF000000),
                unreadBadgeText = Color(0xFFFFFFFF),
                divider = Color(0xFF000000),
                success = Color(0xFF008000),
                warning = Color(0xFFC05000),
                selectedNavContainer = Color(0xFFDDDDDD)
            ),
            isDark = false
        )

        SweetAppearance.WARM_PAPER -> AppearanceColors(
            colorScheme = lightColorScheme(
                primary = Color(0xFF8C4303),
                onPrimary = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFDF9),
                onSurface = Color(0xFF2C221E),
                surfaceVariant = Color(0xFFF5EFE6),
                onSurfaceVariant = Color(0xFF5C4A42),
                background = Color(0xFFFAF6EE),
                onBackground = Color(0xFF2C221E),
                outline = Color(0xFFD6C7B8)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFFF2EBE1),
                bubbleOutgoing = Color(0xFFE8D5C4),
                bubbleIncomingText = Color(0xFF2C221E),
                bubbleOutgoingText = Color(0xFF3D1E0B),
                onlineIndicator = Color(0xFF5B8266),
                unreadBadge = Color(0xFF8C4303),
                unreadBadgeText = Color(0xFFFFFFFF),
                divider = Color(0xFFE3D5C8),
                success = Color(0xFF436B4D),
                warning = Color(0xFFB5651D),
                selectedNavContainer = Color(0xFFEAD8C8)
            ),
            isDark = false
        )

        SweetAppearance.CYBERPUNK -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFF00F0FF),
                onPrimary = Color(0xFF000000),
                surface = Color(0xFF120E16),
                onSurface = Color(0xFF00F0FF),
                surfaceVariant = Color(0xFF1F1A24),
                onSurfaceVariant = Color(0xFFFF2BD6),
                background = Color(0xFF09060C),
                onBackground = Color(0xFF00F0FF),
                outline = Color(0xFFFF2BD6)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF1F1A24),
                bubbleOutgoing = Color(0xFF8B5CF6),
                bubbleIncomingText = Color(0xFF00F0FF),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF00F0FF),
                unreadBadge = Color(0xFFFF2BD6),
                unreadBadgeText = Color(0xFF000000),
                divider = Color(0xFFFF2BD6),
                success = Color(0xFF00F0FF),
                warning = Color(0xFFFFE600),
                selectedNavContainer = Color(0xFF3B0066)
            ),
            isDark = true
        )
    }
}

private fun getCatppuccinColors(variant: CatppuccinVariant): AppearanceColors {
    return when (variant) {
        CatppuccinVariant.LATTE -> AppearanceColors(
            colorScheme = lightColorScheme(
                primary = Color(0xFF8839EF),
                onPrimary = Color(0xFFFFFFFF),
                surface = Color(0xFFEFF1F5),
                onSurface = Color(0xFF4C4F69),
                surfaceVariant = Color(0xFFE6E9EF),
                onSurfaceVariant = Color(0xFF5C5F77),
                background = Color(0xFFE6E9EF),
                onBackground = Color(0xFF4C4F69),
                outline = Color(0xFFBCC0CC)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFFCCD0DA),
                bubbleOutgoing = Color(0xFF1E66F5),
                bubbleIncomingText = Color(0xFF4C4F69),
                bubbleOutgoingText = Color(0xFFFFFFFF),
                onlineIndicator = Color(0xFF40A02B),
                unreadBadge = Color(0xFF8839EF),
                unreadBadgeText = Color(0xFFFFFFFF),
                divider = Color(0xFFBCC0CC),
                success = Color(0xFF40A02B),
                warning = Color(0xFFDF8E1D),
                selectedNavContainer = Color(0xFF8839EF)
            ),
            isDark = false
        )

        CatppuccinVariant.FRAPPE -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFCA9EE6),
                onPrimary = Color(0xFF303446),
                surface = Color(0xFF303446),
                onSurface = Color(0xFFC6D0F5),
                surfaceVariant = Color(0xFF414559),
                onSurfaceVariant = Color(0xFFB5BFE2),
                background = Color(0xFF292C3C),
                onBackground = Color(0xFFC6D0F5),
                outline = Color(0xFF626880)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF414559),
                bubbleOutgoing = Color(0xFF8CAAEE),
                bubbleIncomingText = Color(0xFFC6D0F5),
                bubbleOutgoingText = Color(0xFF303446),
                onlineIndicator = Color(0xFFA6D189),
                unreadBadge = Color(0xFFCA9EE6),
                unreadBadgeText = Color(0xFF303446),
                divider = Color(0xFF51576D),
                success = Color(0xFFA6D189),
                warning = Color(0xFFE5C890),
                selectedNavContainer = Color(0xFF51576D)
            ),
            isDark = true
        )

        CatppuccinVariant.MACCHIATO -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFC6A0F6),
                onPrimary = Color(0xFF24273A),
                surface = Color(0xFF24273A),
                onSurface = Color(0xFFCAD3F5),
                surfaceVariant = Color(0xFF363A4F),
                onSurfaceVariant = Color(0xFFB8C0E0),
                background = Color(0xFF1E2030),
                onBackground = Color(0xFFCAD3F5),
                outline = Color(0xFF5B6078)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF363A4F),
                bubbleOutgoing = Color(0xFF8AADF4),
                bubbleIncomingText = Color(0xFFCAD3F5),
                bubbleOutgoingText = Color(0xFF24273A),
                onlineIndicator = Color(0xFFA6DA95),
                unreadBadge = Color(0xFFC6A0F6),
                unreadBadgeText = Color(0xFF24273A),
                divider = Color(0xFF494D64),
                success = Color(0xFFA6DA95),
                warning = Color(0xFFEED49F),
                selectedNavContainer = Color(0xFF494D64)
            ),
            isDark = true
        )

        CatppuccinVariant.MOCHA -> AppearanceColors(
            colorScheme = darkColorScheme(
                primary = Color(0xFFCBA6F7),
                onPrimary = Color(0xFF1E1E2E),
                surface = Color(0xFF1E1E2E),
                onSurface = Color(0xFFCDD6F4),
                surfaceVariant = Color(0xFF313244),
                onSurfaceVariant = Color(0xFFBAC2DE),
                background = Color(0xFF181825),
                onBackground = Color(0xFFCDD6F4),
                outline = Color(0xFF585B70)
            ),
            extendedColors = SweetExtendedColors(
                bubbleIncoming = Color(0xFF313244),
                bubbleOutgoing = Color(0xFF89B4FA),
                bubbleIncomingText = Color(0xFFCDD6F4),
                bubbleOutgoingText = Color(0xFF1E1E2E),
                onlineIndicator = Color(0xFFA6E3A1),
                unreadBadge = Color(0xFFCBA6F7),
                unreadBadgeText = Color(0xFF1E1E2E),
                divider = Color(0xFF45475A),
                success = Color(0xFFA6E3A1),
                warning = Color(0xFFF9E2AF),
                selectedNavContainer = Color(0xFF45475A)
            ),
            isDark = true
        )
    }
}
