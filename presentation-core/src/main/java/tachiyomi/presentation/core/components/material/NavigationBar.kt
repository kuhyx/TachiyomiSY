package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

private val NavigationBarHeight: Dp = 80.dp

/**
 * M3 Navbar with no horizontal spacer; nullable parameters take the Material defaults.
 *
 * @see [androidx.compose.material3.NavigationBar]
 *
 * `containerColor`: the bar's colour; Material's container colour when `Unspecified`.
 * `contentColor`: the icon and label colour; the matching content colour when `Unspecified`.
 * `tonalElevation`: the tonal elevation; Material's when `Unspecified`.
 */
@Composable
public fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tonalElevation: Dp = Dp.Unspecified,
    windowInsets: WindowInsets? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val container = containerColor.takeOrElse { NavigationBarDefaults.containerColor }
    androidx.compose.material3.Surface(
        color = container,
        contentColor = contentColor.takeOrElse { MaterialTheme.colorScheme.contentColorFor(container) },
        tonalElevation = if (tonalElevation.isSpecified) tonalElevation else NavigationBarDefaults.Elevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets ?: NavigationBarDefaults.windowInsets)
                .height(NavigationBarHeight)
                .selectableGroup(),
            content = content,
        )
    }
}
