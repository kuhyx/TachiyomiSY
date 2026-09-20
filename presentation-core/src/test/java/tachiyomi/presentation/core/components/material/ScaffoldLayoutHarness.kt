package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val FAB_SIZE: Dp = 56.dp
internal val BAR_HEIGHT: Dp = 40.dp
internal val START_BAR_WIDTH: Dp = 60.dp
internal val SNACKBAR_WIDTH: Dp = 100.dp
internal val SNACKBAR_HEIGHT: Dp = 30.dp
internal const val FAB_SPACING = 16
internal const val LEFT_INSET = 10
internal const val TOP_INSET = 20
internal const val RIGHT_INSET = 12
internal const val BOTTOM_INSET = 8

/** The insets every harness pass keeps clear of, in dp. */
internal val harnessInsets: WindowInsets = WindowInsets(
    left = LEFT_INSET.dp,
    top = TOP_INSET.dp,
    right = RIGHT_INSET.dp,
    bottom = BOTTOM_INSET.dp,
)

/** A [ScaffoldLayout] whose slots are tagged boxes of known size, wrapped in the theme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScaffoldHarness(
    fabPosition: FabPosition = FabPosition.End,
    hasTopBar: Boolean = false,
    hasStartBar: Boolean = false,
    hasSnackbar: Boolean = false,
    fab: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    onPadding: (PaddingValues) -> Unit = {},
) {
    MaterialTheme {
        ScaffoldLayout(
            fabPosition = fabPosition,
            topBar = { if (hasTopBar) TaggedBox(tag = "top", width = null, height = BAR_HEIGHT) },
            startBar = { if (hasStartBar) TaggedBox(tag = "start", width = START_BAR_WIDTH, height = null) },
            content = { padding -> onPadding(padding) },
            snackbar = {
                if (hasSnackbar) TaggedBox(tag = "snackbar", width = SNACKBAR_WIDTH, height = SNACKBAR_HEIGHT)
            },
            fab = fab,
            contentWindowInsets = harnessInsets,
            bottomBar = bottomBar,
        )
    }
}

/** A full-width bottom bar stand-in of [height]. */
@Composable
internal fun BottomBar(height: Dp = BAR_HEIGHT) {
    TaggedBox(tag = "bottom", width = null, height = height)
}

/** A square FAB stand-in. */
@Composable
internal fun Fab() {
    TaggedBox(tag = "fab", width = FAB_SIZE, height = FAB_SIZE)
}

/** A box of [width] x [height]; a `null` dimension fills the parent instead. */
@Composable
internal fun TaggedBox(tag: String, width: Dp?, height: Dp?) {
    val sized = when {
        width == null -> Modifier.fillMaxWidth().height(checkNotNull(height))
        height == null -> Modifier.width(width)
        else -> Modifier.size(width = width, height = height)
    }
    Box(sized.testTag(tag))
}
