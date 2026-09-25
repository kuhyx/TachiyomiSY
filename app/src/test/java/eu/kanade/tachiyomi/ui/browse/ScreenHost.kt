package eu.kanade.tachiyomi.ui.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen

/**
 * Composes [screen] on top of a blank one and keeps composing it whatever gets pushed or popped,
 * so a test can click through it and read the navigator's stack afterwards.
 */
internal class ScreenHost(private val screen: Screen) {
    lateinit var navigator: Navigator

    fun show(compose: ComposeContentTestRule) {
        compose.setContent {
            MaterialTheme {
                Navigator(listOf(BlankScreen(), screen)) { current ->
                    navigator = current
                    screen.Content()
                }
            }
        }
        compose.waitForIdle()
    }

    /** What the navigator shows on top now. */
    val top: Screen get() = navigator.lastItem
}
