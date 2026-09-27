package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.disposeScreenModels
import eu.kanade.tachiyomi.ui.base.pollGone
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.library.LibraryTabRig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * [HomeScreen] hosted as the activity hosts it, over the library tab's rig: the library is the start
 * tab, and anything the home screen pushes shows up as `opened:<ClassName>`.
 */
internal class HomeRig(
    private val compose: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {
    val library: LibraryTabRig = LibraryTabRig(compose)
    val ui: UiPreferences get() = UiPreferences(library.harness.store)
    private val sender = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() = library.start()

    fun stop() {
        sender.cancel()
        disposeScreenModels(HomeScreen, *HomeScreen.TABS.toTypedArray())
        library.stop()
    }

    fun show() {
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent { ScreenHost(HomeScreen) }
        compose.pollLabel("Library")
    }

    /** Sends [block]'s event from off the main thread; the home screen receives it on the main thread. */
    fun send(block: suspend () -> Unit) {
        sender.launch { block() }
    }

    fun await(label: String) = compose.pollLabel(label)

    fun awaitGone(label: String) = compose.pollGone(label)

    /** Clicks the navigation item whose icon is described as [title]. */
    fun tab(title: String) {
        compose.onNode(hasContentDescription(title), useUnmergedTree = true).performClick()
        compose.waitForIdle()
    }
}
