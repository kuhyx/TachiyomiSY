package tachiyomi.presentation.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider

/** The Samsung cover-screen updates widget: white on the cover-screen background, padded at the bottom. */
public class UpdatesGridCoverScreenGlanceWidget : BaseUpdatesGridGlanceWidget() {
    override val foreground: ColorProvider = ColorProvider(Color.White)
    override val background: ImageProvider = ImageProvider(R.drawable.appwidget_coverscreen_background)
    override val topPadding: Dp = 0.dp
    override val bottomPadding: Dp = COVER_SCREEN_BOTTOM_EDGE_CLEARANCE
}

private val COVER_SCREEN_BOTTOM_EDGE_CLEARANCE: Dp = 24.dp
