package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.manga.MangaScreenActions
import eu.kanade.tachiyomi.ui.manga.merged.themedActivity
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen

/**
 * Composes [MangaScreen.mangaScreenActions] under a navigator that renders nothing, so a test can
 * invoke each callback and read the navigator's stack without composing the pushed screens.
 */
internal class MangaActionsHost(private val compose: ComposeContentTestRule) {
    val context = themedActivity()
    lateinit var actions: MangaScreenActions
    lateinit var navigator: Navigator

    fun show(model: MangaScreenModel, screen: MangaScreen = MangaScreen(1L)) {
        val state = model.awaitSuccess()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                Navigator(BlankScreen()) { current ->
                    navigator = current
                    actions = screen.mangaScreenActions(model, state)
                }
            }
        }
        compose.waitForIdle()
    }

    /** The screen on top of the stack after [block] ran. */
    fun pushedBy(block: () -> Unit): Any {
        block()
        compose.waitForIdle()
        return navigator.lastItem
    }
}
