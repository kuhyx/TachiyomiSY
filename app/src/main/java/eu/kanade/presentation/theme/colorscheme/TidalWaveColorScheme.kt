package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Tidal Wave theme
 * Original color scheme by NahutabDevelop
 *
 * Key colors:
 * Primary #004152
 * Secondary #5ed4fc
 * Tertiary #92f7bc
 * Neutral #16151D
 */
internal object TidalWaveColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(color = 0xFF5ed4fc),
        onPrimary = Color(color = 0xFF003544),
        primaryContainer = Color(color = 0xFF004d61),
        onPrimaryContainer = Color(color = 0xFFb8eaff),
        inversePrimary = Color(color = 0xFFa12b03),
        secondary = Color(color = 0xFF5ed4fc), // Unread badge
        onSecondary = Color(color = 0xFF003544), // Unread badge text
        secondaryContainer = Color(color = 0xFF004d61), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFFb8eaff), // Navigation bar selector icon
        tertiary = Color(color = 0xFF92f7bc), // Downloaded badge
        onTertiary = Color(color = 0xFF001c3b), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFFc3fada),
        onTertiaryContainer = Color(color = 0xFF78ffd6),
        background = Color(color = 0xFF001c3b),
        onBackground = Color(color = 0xFFd5e3ff),
        surface = Color(color = 0xFF001c3b),
        onSurface = Color(color = 0xFFd5e3ff),
        surfaceVariant = Color(color = 0xFF082b4b), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFFbfc8cc),
        surfaceTint = Color(color = 0xFF5ed4fc),
        inverseSurface = Color(color = 0xFFffe3c4),
        inverseOnSurface = Color(color = 0xFF001c3b),
        outline = Color(color = 0xFF8a9296),
        surfaceContainerLowest = Color(color = 0xFF072642),
        surfaceContainerLow = Color(color = 0xFF072947),
        surfaceContainer = Color(color = 0xFF082b4b), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFF093257),
        surfaceContainerHighest = Color(color = 0xFF0A3861),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(color = 0xFF006780),
        onPrimary = Color(color = 0xFFffffff),
        primaryContainer = Color(color = 0xFFB4D4DF),
        onPrimaryContainer = Color(color = 0xFF001f28),
        inversePrimary = Color(color = 0xFFff987f),
        secondary = Color(color = 0xFF006780), // Unread badge
        onSecondary = Color(color = 0xFFffffff), // Unread badge text
        secondaryContainer = Color(color = 0xFF9AE1FF), // Navigation bar selector pill & progress indicator (remaining)
        onSecondaryContainer = Color(color = 0xFF001f28), // Navigation bar selector icon
        tertiary = Color(color = 0xFF92f7bc), // Downloaded badge
        onTertiary = Color(color = 0xFF001c3b), // Downloaded badge text
        tertiaryContainer = Color(color = 0xFFc3fada),
        onTertiaryContainer = Color(color = 0xFF78ffd6),
        background = Color(color = 0xFFfdfbff),
        onBackground = Color(color = 0xFF001c3b),
        surface = Color(color = 0xFFfdfbff),
        onSurface = Color(color = 0xFF001c3b),
        surfaceVariant = Color(color = 0xFFe8eff5), // Navigation bar background (ThemePrefWidget)
        onSurfaceVariant = Color(color = 0xFF40484c),
        surfaceTint = Color(color = 0xFF006780),
        inverseSurface = Color(color = 0xFF020400),
        inverseOnSurface = Color(color = 0xFFffe3c4),
        outline = Color(color = 0xFF70787c),
        surfaceContainerLowest = Color(color = 0xFFe2e8ec),
        surfaceContainerLow = Color(color = 0xFFe5ecf1),
        surfaceContainer = Color(color = 0xFFe8eff5), // Navigation bar background
        surfaceContainerHigh = Color(color = 0xFFedf4fA),
        surfaceContainerHighest = Color(color = 0xFFf5faff),
    )
}
