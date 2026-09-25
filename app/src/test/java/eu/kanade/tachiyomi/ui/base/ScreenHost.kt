package eu.kanade.tachiyomi.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator

/**
 * Hosts [root] in a Navigator but composes only screens the test owns: anything the screen pushes or
 * replaces itself with is shown as `opened:<ClassName>` instead of being composed with its own model.
 */
@Composable
internal fun ScreenHost(root: Screen, owned: (Screen) -> Boolean = { it === root }) {
    MaterialTheme {
        Navigator(root) { navigator ->
            val top = navigator.lastItem
            if (owned(top)) CurrentScreen() else Text("opened:${top::class.simpleName}")
        }
    }
}

/** Hosts [tab] as the home screen would, its title shown as `tab:<title>` and pushed screens as text. */
@Composable
internal fun TabHost(tab: Tab) {
    val root = TabRootScreen(tab)
    ScreenHost(root)
}

private class TabRootScreen(private val tab: Tab) : Screen {
    @Composable
    override fun Content() {
        // As HomeScreen does: the tab's content pushes onto the outer navigator, not the tab navigator.
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(tab) {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Text("tab:${tab.options.title}")
                CurrentTab()
            }
        }
    }
}
