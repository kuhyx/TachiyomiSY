package mihon.core.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
@ReadOnlyComposable
internal fun isMediumWidthWindow(): Boolean = windowContainerWidth() > MediumWidthWindowSize

@Composable
@ReadOnlyComposable
internal fun isExpandedWidthWindow(): Boolean = windowContainerWidth() > ExpandedWidthWindowSize

@Composable
@ReadOnlyComposable
private fun windowContainerWidth(): Dp =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

internal val MediumWidthWindowSize = 600.dp
internal val ExpandedWidthWindowSize = 840.dp
