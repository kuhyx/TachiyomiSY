package tachiyomi.presentation.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.util.dayNightColorResource

/** The home-screen updates widget: themed background, no extra padding. */
public class UpdatesGridGlanceWidget : BaseUpdatesGridGlanceWidget() {
    override val foreground: ColorProvider = context.dayNightColorResource(R.color.appwidget_on_secondary_container)
    override val background: ImageProvider = ImageProvider(R.drawable.appwidget_background)
    override val topPadding: Dp = 0.dp
    override val bottomPadding: Dp = 0.dp
}
