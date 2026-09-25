package eu.kanade.tachiyomi.ui.browse

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import eu.kanade.presentation.util.Screen as AppScreen

/**
 * Composes one browse tab's content under a navigator that never renders what gets pushed, so a
 * test can click through the tab and read the navigator's stack.
 */
internal class TabHost(private val tab: @Composable Screen.() -> TabContent) : AppScreen() {
    lateinit var content: TabContent
    lateinit var navigator: Navigator
    var back: OnBackPressedDispatcher? = null
    val snackbar: SnackbarHostState = SnackbarHostState()

    @Composable
    override fun Content() {
        back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        content = tab()
        content.content(PaddingValues(), snackbar)
    }

    fun show(compose: ComposeContentTestRule) {
        compose.setContent {
            MaterialTheme {
                Navigator(BlankScreen()) { current ->
                    navigator = current
                    Content()
                }
            }
        }
        compose.waitForIdle()
    }

    /** Presses the system back button. */
    fun pressBack(compose: ComposeContentTestRule) {
        compose.runOnUiThread { back?.onBackPressed() }
        compose.waitForIdle()
    }

    /** Runs the app bar action titled [title]. */
    fun action(title: String) {
        when (val action = content.actions.first { it.title() == title }) {
            is AppBar.Action -> action.onClick()
            is AppBar.OverflowAction -> action.onClick()
        }
    }

    private fun AppBar.AppBarAction.title(): String = when (this) {
        is AppBar.Action -> title
        is AppBar.OverflowAction -> title
    }
}
