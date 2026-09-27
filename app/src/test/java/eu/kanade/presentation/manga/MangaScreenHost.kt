package eu.kanade.presentation.manga

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import org.koin.core.module.Module
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction
import java.time.Instant

/**
 * Composes the whole [MangaScreen] from mutable inputs, so a test can swap the state, the layout or
 * the actions and recompose; [churn] recomposes once unchanged and once with a new state.
 */
internal class MangaScreenHost(private val compose: AndroidComposeTestRule<*, ComponentActivity>) {
    val koin: PresentationKoin = PresentationKoin()
    val recorder: RecordingActions = RecordingActions()
    val events: MutableList<String> get() = recorder.events
    var state: MangaScreenModel.State.Success by mutableStateOf(screenState())
    var tablet: Boolean by mutableStateOf(false)
    var actions: MangaScreenActions by mutableStateOf(recorder.build())
    var nextUpdate: Instant? by mutableStateOf(null)
    var swipeStart: ChapterSwipeAction by mutableStateOf(ChapterSwipeAction.ToggleRead)
    var swipeEnd: ChapterSwipeAction by mutableStateOf(ChapterSwipeAction.Download)
    private var tick by mutableIntStateOf(0)
    private val snackbar = SnackbarHostState()

    fun start(vararg extra: Module) {
        koin.startWithPreferences(*extra)
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
    }

    fun stop() {
        koin.stop()
    }

    fun show(initial: MangaScreenModel.State.Success, isTablet: Boolean = false) {
        state = initial
        tablet = isTablet
        compose.setContent {
            MaterialTheme {
                Text("t$tick")
                MangaScreen(
                    state = state,
                    snackbarHostState = snackbar,
                    nextUpdate = nextUpdate,
                    isTabletUi = tablet,
                    chapterSwipeStartAction = swipeStart,
                    chapterSwipeEndAction = swipeEnd,
                    actions = actions,
                )
            }
        }
        compose.waitForIdle()
    }

    /** Taps the [label]ed node nearest the bottom ([lowest]) or the top of the screen. */
    fun click(label: String, lowest: Boolean) {
        val nodes = compose.onAllNodesWithContentDescription(label)
        val tops = nodes.fetchSemanticsNodes().map { it.boundsInRoot.top }
        val target = if (lowest) tops.max() else tops.min()
        nodes[tops.indexOf(target)].performClick()
    }

    fun count(label: String): Int = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().size

    fun churn(next: MangaScreenModel.State.Success) {
        tick++
        compose.waitForIdle()
        state = next
        compose.waitForIdle()
    }
}
