package tachiyomi.presentation.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val AMBER_200: Long = 0xFFFFEB3B
private const val AMBER_500: Long = 0xFFFFC107
private val ActiveDark = Color(AMBER_200)
private val ActiveLight = Color(AMBER_500)

/** The amber that marks an active item: lighter in the dark theme. */
public val ColorScheme.active: Color
    @Composable
    get() = if (isSystemInDarkTheme()) ActiveDark else ActiveLight
