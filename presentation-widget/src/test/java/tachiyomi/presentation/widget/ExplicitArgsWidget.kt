package tachiyomi.presentation.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.unit.ColorProvider

/** Space kept clear above the grid by [ExplicitArgsWidget]. */
internal val EXPLICIT_TOP_PADDING: Dp = 8.dp

/** Space kept clear below the grid by [ExplicitArgsWidget]. */
internal val EXPLICIT_BOTTOM_PADDING: Dp = 4.dp

/**
 * A grid widget that passes every constructor argument of [BaseUpdatesGridGlanceWidget] explicitly
 * (the shipped widgets rely on the Injekt defaults), with padding on both edges.
 */
internal class ExplicitArgsWidget : BaseUpdatesGridGlanceWidget(
    WidgetInjekt.application,
    WidgetInjekt.getUpdates,
    WidgetInjekt.preferences,
) {
    override val foreground: ColorProvider = ColorProvider(Color.Black)
    override val background: ImageProvider = ImageProvider(R.drawable.appwidget_background)
    override val topPadding: Dp = EXPLICIT_TOP_PADDING
    override val bottomPadding: Dp = EXPLICIT_BOTTOM_PADDING
}

/** The receiver `GlanceAppWidgetManager` maps to [ExplicitArgsWidget], so its instances can be placed. */
internal class ExplicitArgsReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = ExplicitArgsWidget()
}
