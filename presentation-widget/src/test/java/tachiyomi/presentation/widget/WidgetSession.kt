package tachiyomi.presentation.widget

import android.content.Context
import android.os.Looper
import android.util.SizeF
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.glance.Applier
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.AppWidgetSession
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.RemoteViewsRoot
import androidx.glance.testing.GlanceNode
import androidx.glance.testing.GlanceNodeMatcher
import androidx.glance.testing.unit.GlanceMappedNode
import androidx.glance.testing.unit.MappedNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.robolectric.Shadows.shadowOf

private const val MAX_TREE_DEPTH = 50
private const val WAIT_ATTEMPTS = 200
private const val WAIT_STEP_MS = 25L

/** The live emittable tree of a running widget session. */
internal class WidgetSessionScope(private val root: RemoteViewsRoot) {

    /**
     * Runs [assertion] against every node of the tree until it passes, pumping the main looper
     * (where the session composes and where the DataStore and Coil hops land) between attempts;
     * rethrows the last failure after a bounded number of attempts so nothing can hang the suite.
     */
    fun awaitUntil(assertion: (List<GlanceNode<MappedNode>>) -> Unit) {
        var failure: AssertionError? = null
        repeat(WAIT_ATTEMPTS) {
            Snapshot.sendApplyNotifications()
            shadowOf(Looper.getMainLooper()).idle()
            try {
                assertion(GlanceMappedNode(root).flatten())
                return
            } catch (expected: AssertionError) {
                failure = expected
                Thread.sleep(WAIT_STEP_MS)
            }
        }
        throw checkNotNull(failure)
    }
}

/** How many of the nodes match [matcher]. */
internal fun List<GlanceNode<MappedNode>>.matching(matcher: GlanceNodeMatcher<MappedNode>): Int =
    count { matcher.matches(it) }

private fun GlanceNode<MappedNode>.flatten(): List<GlanceNode<MappedNode>> =
    listOf(this) + children().flatMap { it.flatten() }

/** A frame clock that runs every frame request at once, like the Glance test harness's. */
private object ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R = onFrame(System.nanoTime())
}

/**
 * Composes [widget] the way Glance itself would for app widget [appWidgetId], which [placeWidget]
 * binds to [receiver] at [size] first: `AppWidgetSession.provideGlance` is the composable a session
 * worker composes, and it runs the widget's own `provideGlance` (Injekt defaults, the sizes placed
 * in `AppWidgetManager`, `provideContent`) on Glance's emittable tree, driven by Robolectric's main
 * looper instead of a WorkManager job the test JVM cannot run. The tree is read live because
 * `runGlanceAppWidgetUnitTest` only copies its tree when it sees the recomposer go idle, which a
 * recomposition finished within one scheduler drain never shows it. The Compose UI context is
 * supplied for the reason given at [composeUiContext].
 */
internal fun runWidgetSession(
    context: Context,
    widget: BaseUpdatesGridGlanceWidget,
    receiver: Class<out GlanceAppWidgetReceiver>,
    appWidgetId: Int,
    size: SizeF,
    assertions: WidgetSessionScope.() -> Unit,
) {
    placeWidget(context = context, receiver = receiver, appWidgetId = appWidgetId, size = size)
    val content = AppWidgetSession(widget, AppWidgetId(appWidgetId)).provideGlance(context)
    val root = RemoteViewsRoot(MAX_TREE_DEPTH)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + ImmediateFrameClock)
    val recomposer = Recomposer(scope.coroutineContext)
    val composition = Composition(Applier(root), recomposer)
    scope.launch { recomposer.runRecomposeAndApplyChanges() }
    try {
        composition.setContent {
            CompositionLocalProvider(composeUiContext(context)) { content() }
        }
        WidgetSessionScope(root).assertions()
    } finally {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
    }
}
