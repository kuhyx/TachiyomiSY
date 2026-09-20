package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Lavender theme
 * Color scheme by Osyx.
 *
 * Key colors:
 * Primary #A177FF
 * Secondary #A177FF
 * Tertiary #5E25E1
 * Neutral #111129
 */
internal object LavenderColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(color = 0xFFA177FF),
        onPrimary = Color(color = 0xFF3D0090),
        primaryContainer = Color(color = 0xFFA177FF),
        onPrimaryContainer = Color(color = 0xFFFFFFFF),
        secondary = Color(color = 0xFFA177FF), // Unread badge
        onSecondary = Color(color = 0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(color = 0xFF423271), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFFA177FF), // Navigation bar selected icon
        tertiary = Color(color = 0xFFCDBDFF), // Downloaded badge
        onTertiary = Color(color = 0xFF360096), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF5512D8),
        onTertiaryContainer = Color(color = 0xFFEFE6FF),
        error = Color(color = 0xFFFFB4AB),
        onError = Color(color = 0xFF690005),
        errorContainer = Color(color = 0xFF93000A),
        onErrorContainer = Color(color = 0xFFFFDAD6),
        background = Color(color = 0xFF111129),
        onBackground = Color(color = 0xFFE7E0EC),
        surface = Color(color = 0xFF111129),
        onSurface = Color(color = 0xFFE7E0EC),
        surfaceVariant = Color(color = 0xFF3D2F6B), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFFCBC3D6),
        outline = Color(color = 0xFF958E9F),
        outlineVariant = Color(color = 0xFF4A4453),
        scrim = Color(color = 0xFF000000),
        inverseSurface = Color(color = 0xFFE7E0EC),
        inverseOnSurface = Color(color = 0xFF322F38),
        inversePrimary = Color(color = 0xFF6D41C8),
        surfaceDim = Color(color = 0xFF111129),
        surfaceBright = Color(color = 0xFF3B3841),
        surfaceContainerLowest = Color(color = 0xFF15132d),
        surfaceContainerLow = Color(color = 0xFF171531),
        surfaceContainer = Color(color = 0xFF1D193B), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFF241f41),
        surfaceContainerHighest = Color(color = 0xFF282446),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(color = 0xFF6D41C8),
        onPrimary = Color(color = 0xFFFFFFFF),
        primaryContainer = Color(color = 0xFF7B46AF),
        onPrimaryContainer = Color(color = 0xFF130038),
        secondary = Color(color = 0xFF7B46AF), // Unread badge
        onSecondary = Color(color = 0xFFEDE2FF), // Unread badge text
        secondaryContainer = Color(color = 0xFFC9B0E6), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF7B46AF), // Navigation bar selector icon
        tertiary = Color(color = 0xFFEDE2FF), // Downloaded badge
        onTertiary = Color(color = 0xFF7B46AF), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF6D3BF0),
        onTertiaryContainer = Color(color = 0xFFFFFFFF),
        error = Color(color = 0xFFBA1A1A),
        onError = Color(color = 0xFFFFFFFF),
        errorContainer = Color(color = 0xFFFFDAD6),
        onErrorContainer = Color(color = 0xFF410002),
        background = Color(color = 0xFFEDE2FF),
        onBackground = Color(color = 0xFF1D1A22),
        surface = Color(color = 0xFFEDE2FF),
        onSurface = Color(color = 0xFF1D1A22),
        surfaceVariant = Color(color = 0xFFE4D5F8), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFF4A4453),
        outline = Color(color = 0xFF7B7485),
        outlineVariant = Color(color = 0xFFCBC3D6),
        scrim = Color(color = 0xFF000000),
        inverseSurface = Color(color = 0xFF322F38),
        inverseOnSurface = Color(color = 0xFFF5EEFA),
        inversePrimary = Color(color = 0xFFA177FF),
        surfaceDim = Color(color = 0xFFDED7E3),
        surfaceBright = Color(color = 0xFFEDE2FF),
        surfaceContainerLowest = Color(color = 0xFFDACCEC),
        surfaceContainerLow = Color(color = 0xFFDED0F1),
        surfaceContainer = Color(color = 0xFFE4D5F8), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFFEADCFD),
        surfaceContainerHighest = Color(color = 0xFFEEE2FF),
    )
}
