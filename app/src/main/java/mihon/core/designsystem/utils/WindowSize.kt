package mihon.core.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
@ReadOnlyComposable
internal fun isMediumWidthWindow(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > MediumWidthWindowSize.value
}

@Composable
@ReadOnlyComposable
internal fun isExpandedWidthWindow(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > ExpandedWidthWindowSize.value
}

internal val MediumWidthWindowSize = 600.dp
internal val ExpandedWidthWindowSize = 840.dp
