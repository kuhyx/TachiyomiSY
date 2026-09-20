package tachiyomi.presentation.widget

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.presentation.widget.components.LockedWidget
import tachiyomi.presentation.widget.components.UpdatesWidget
import tachiyomi.presentation.widget.util.appWidgetBackgroundRadius
import tachiyomi.presentation.widget.util.calculateRowAndColumnCount
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZonedDateTime

private const val UPDATES_WINDOW_MONTHS: Long = 3

/**
 * A grid of the covers with recent unread chapters, or a "locked" notice while the app lock is
 * on; subclasses pick the colours and the padding of their placement.
 */
public abstract class BaseUpdatesGridGlanceWidget(
    /** The application context the widget resolves its resources with. */
    protected val context: Context = Injekt.get<Application>(),
    private val getUpdates: GetUpdates = Injekt.get(),
    private val preferences: SecurityPreferences = Injekt.get(),
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    /** The text and progress colour. */
    public abstract val foreground: ColorProvider

    /** The widget's background image. */
    public abstract val background: ImageProvider

    /** Extra space kept clear above the grid. */
    public abstract val topPadding: Dp

    /** Extra space kept clear below the grid. */
    public abstract val bottomPadding: Dp

    private val coverLoader by lazy { UpdatesCoverLoader(context) }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val locked = preferences.useAuthenticator.get()
        val containerModifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .appWidgetBackground()
            .padding(top = topPadding, bottom = bottomPadding)
            .appWidgetBackgroundRadius()

        val (rowCount, columnCount) = largestPlacedWidgetSize(context)
            .calculateRowAndColumnCount(topPadding, bottomPadding)

        provideContent {
            Content(locked, containerModifier, rowCount, columnCount)
        }
    }

    private suspend fun largestPlacedWidgetSize(context: Context): DpSize {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(javaClass)
            .flatMap { manager.getAppWidgetSizes(it) }
            .maxBy { it.height.value * it.width.value }
    }

    @Composable
    private fun Content(locked: Boolean, containerModifier: GlanceModifier, rowCount: Int, columnCount: Int) {
        // If app lock enabled, don't do anything
        if (locked) {
            LockedWidget(
                foreground = foreground,
                modifier = containerModifier,
            )
        } else {
            val flow = remember { updatesCovers(rowCount, columnCount) }
            val data by flow.collectAsState(initial = null)
            UpdatesWidget(
                data = data,
                contentColor = foreground,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                modifier = containerModifier,
            )
        }
    }

    private fun updatesCovers(rowCount: Int, columnCount: Int): Flow<List<Pair<Long, Bitmap?>>> = getUpdates
        .subscribe(false, DateLimit.toEpochMilli())
        .map { rawData -> coverLoader.load(rawData, rowCount, columnCount) }

    /** The window of updates the widgets show. */
    public companion object {
        /** Now minus three months: updates older than this are not shown. */
        public val DateLimit: Instant
            get() = ZonedDateTime.now().minusMonths(UPDATES_WINDOW_MONTHS).toInstant()
    }
}
