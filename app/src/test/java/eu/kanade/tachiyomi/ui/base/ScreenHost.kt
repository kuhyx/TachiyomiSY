package eu.kanade.tachiyomi.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator

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
