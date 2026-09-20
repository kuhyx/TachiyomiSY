package tachiyomi.presentation.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** The broadcast receiver behind the Samsung cover-screen updates widget. */
public class UpdatesGridCoverScreenGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = UpdatesGridCoverScreenGlanceWidget()
}
