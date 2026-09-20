package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Nord theme
 * https://www.nordtheme.com/docs/colors-and-palettes
 * for the light theme, the primary color is switched with the tertiary for better contrast in some case.
 */
internal object NordColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(color = 0xFF88C0D0),
        onPrimary = Color(color = 0xFF2E3440),
        primaryContainer = Color(color = 0xFF88C0D0),
        onPrimaryContainer = Color(color = 0xFF2E3440),
        inversePrimary = Color(color = 0xFF397E91),
        secondary = Color(color = 0xFF81A1C1), // Unread badge
        onSecondary = Color(color = 0xFF2E3440), // Unread badge text
        secondaryContainer = Color(color = 0xFF506275), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF88C0D0), // Navigation bar selector icon
        tertiary = Color(color = 0xFF5E81AC), // Downloaded badge
        onTertiary = Color(color = 0xFF000000), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF5E81AC),
        onTertiaryContainer = Color(color = 0xFF000000),
        background = Color(color = 0xFF2E3440),
        onBackground = Color(color = 0xFFECEFF4),
        surface = Color(color = 0xFF2E3440),
        onSurface = Color(color = 0xFFECEFF4),
        surfaceVariant = Color(color = 0xFF414C5C), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFFECEFF4),
        surfaceTint = Color(color = 0xFF88C0D0),
        inverseSurface = Color(color = 0xFFD8DEE9),
        inverseOnSurface = Color(color = 0xFF2E3440),
        outline = Color(color = 0xFF6d717b),
        outlineVariant = Color(color = 0xFF90939a),
        onError = Color(color = 0xFF2E3440),
        errorContainer = Color(color = 0xFFBF616A),
        onErrorContainer = Color(color = 0xFF000000),
        surfaceContainerLowest = Color(color = 0xFF373F4D),
        surfaceContainerLow = Color(color = 0xFF3E4756),
        surfaceContainer = Color(color = 0xFF414C5C),
        surfaceContainerHigh = Color(color = 0xFF4E5766),
        surfaceContainerHighest = Color(color = 0xFF505968), // Navigation bar background
    )

    override val lightScheme = lightColorScheme(
        primary = Color(color = 0xFF5E81AC),
        onPrimary = Color(color = 0xFF000000),
        primaryContainer = Color(color = 0xFF5E81AC),
        onPrimaryContainer = Color(color = 0xFF000000),
        inversePrimary = Color(color = 0xFF8CA8CD),
        secondary = Color(color = 0xFF81A1C1), // Unread badge
        onSecondary = Color(color = 0xFF2E3440), // Unread badge text
        secondaryContainer = Color(color = 0xFF91B4D7), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF2E3440), // Navigation bar selector icon
        tertiary = Color(color = 0xFF88C0D0), // Downloaded badge
        onTertiary = Color(color = 0xFF2E3440), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFF88C0D0),
        onTertiaryContainer = Color(color = 0xFF2E3440),
        background = Color(color = 0xFFECEFF4),
        onBackground = Color(color = 0xFF2E3440),
        surface = Color(color = 0xFFE5E9F0),
        onSurface = Color(color = 0xFF2E3440),
        surfaceVariant = Color(color = 0xFFDAE0EA), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFF2E3440),
        surfaceTint = Color(color = 0xFF5E81AC),
        inverseSurface = Color(color = 0xFF3B4252),
        inverseOnSurface = Color(color = 0xFFECEFF4),
        outline = Color(color = 0xFF2E3440),
        outlineVariant = Color(color = 0xFFD8DEE9),
        onError = Color(color = 0xFFECEFF4),
        errorContainer = Color(color = 0xFFBF616A),
        onErrorContainer = Color(color = 0xFF000000),
        surfaceContainerLowest = Color(color = 0xFFD1D7E0),
        surfaceContainerLow = Color(color = 0xFFD6DCE6),
        surfaceContainer = Color(color = 0xFFDAE0EA), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFFE9EDF3),
        surfaceContainerHighest = Color(color = 0xFFF2F4F8),
    )
}
