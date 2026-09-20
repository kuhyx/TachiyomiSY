package tachiyomi.presentation.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** The broadcast receiver behind the home-screen updates widget. */
public class UpdatesGridGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = UpdatesGridGlanceWidget()
}
