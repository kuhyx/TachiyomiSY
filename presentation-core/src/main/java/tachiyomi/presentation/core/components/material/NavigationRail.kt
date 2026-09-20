package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val NavigationRailMinWidth: Dp = 80.dp
private val NavigationRailElevation: Dp = 3.dp

/**
 * Center-aligned M3 Navigation rail; nullable parameters take the Material defaults.
 *
 * @see [androidx.compose.material3.NavigationRail]
 *
 * `containerColor`: the rail's colour; Material's container colour when `Unspecified`.
 * `contentColor`: the icon and label colour; the matching content colour when `Unspecified`.
 */
@Composable
public fun NavigationRail(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    windowInsets: WindowInsets? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = containerColor.takeOrElse { NavigationRailDefaults.ContainerColor }
    androidx.compose.material3.Surface(
        color = container,
        contentColor = contentColor.takeOrElse { contentColorFor(container) },
        modifier = modifier,
        tonalElevation = NavigationRailElevation,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(windowInsets ?: NavigationRailDefaults.windowInsets)
                .widthIn(min = NavigationRailMinWidth)
                .padding(vertical = MaterialTheme.padding.extraSmall)
                .selectableGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                MaterialTheme.padding.extraSmall,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            if (header != null) {
                header()
                Spacer(Modifier.height(MaterialTheme.padding.small))
            }
            content()
        }
    }
}
