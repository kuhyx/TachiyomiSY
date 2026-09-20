package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Teal Turqoise theme
 */
internal object TealTurqoiseColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(color = 0xFF40E0D0),
        onPrimary = Color(color = 0xFF000000),
        primaryContainer = Color(color = 0xFF40E0D0),
        onPrimaryContainer = Color(color = 0xFF000000),
        inversePrimary = Color(color = 0xFF008080),
        secondary = Color(color = 0xFF40E0D0), // Unread badge
        onSecondary = Color(color = 0xFF000000), // Unread badge text
        secondaryContainer = Color(color = 0xFF18544E), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF40E0D0), // Navigation bar selector icon
        tertiary = Color(color = 0xFFBF1F2F), // Downloaded badge
        onTertiary = Color(color = 0xFFFFFFFF), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF200508),
        onTertiaryContainer = Color(color = 0xFFBF1F2F),
        background = Color(color = 0xFF202125),
        onBackground = Color(color = 0xFFDFDEDA),
        surface = Color(color = 0xFF202125),
        onSurface = Color(color = 0xFFDFDEDA),
        surfaceVariant = Color(color = 0xFF233133), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFFDFDEDA),
        surfaceTint = Color(color = 0xFF40E0D0),
        inverseSurface = Color(color = 0xFFDFDEDA),
        inverseOnSurface = Color(color = 0xFF202125),
        outline = Color(color = 0xFF899391),
        surfaceContainerLowest = Color(color = 0xFF202C2E),
        surfaceContainerLow = Color(color = 0xFF222F31),
        surfaceContainer = Color(color = 0xFF233133), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFF28383A),
        surfaceContainerHighest = Color(color = 0xFF2F4244),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(color = 0xFF008080),
        onPrimary = Color(color = 0xFFFFFFFF),
        primaryContainer = Color(color = 0xFF008080),
        onPrimaryContainer = Color(color = 0xFFFFFFFF),
        inversePrimary = Color(color = 0xFF40E0D0),
        secondary = Color(color = 0xFF008080), // Unread badge text
        onSecondary = Color(color = 0xFFFFFFFF), // Unread badge text
        secondaryContainer = Color(color = 0xFFCFE5E4), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF008080), // Navigation bar selector icon
        tertiary = Color(color = 0xFFFF7F7F), // Downloaded badge
        onTertiary = Color(color = 0xFF000000), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF2A1616),
        onTertiaryContainer = Color(color = 0xFFFF7F7F),
        background = Color(color = 0xFFFAFAFA),
        onBackground = Color(color = 0xFF050505),
        surface = Color(color = 0xFFFAFAFA),
        onSurface = Color(color = 0xFF050505),
        surfaceVariant = Color(color = 0xFFEBF3F1), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFF050505),
        surfaceTint = Color(color = 0xFFBFDFDF),
        inverseSurface = Color(color = 0xFF050505),
        inverseOnSurface = Color(color = 0xFFFAFAFA),
        outline = Color(color = 0xFF6F7977),
        surfaceContainerLowest = Color(color = 0xFFE1E9E7),
        surfaceContainerLow = Color(color = 0xFFE6EEEC),
        surfaceContainer = Color(color = 0xFFEBF3F1), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFFF0F8F6),
        surfaceContainerHighest = Color(color = 0xFFF7FFFD),
    )
}
