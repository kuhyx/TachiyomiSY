package eu.kanade.tachiyomi.ui.more

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator

/** Hosts [screen] above a blank root, so a pop shows `root` and anything pushed shows `opened:<ClassName>`. */
@Composable
internal fun StackHost(screen: Screen) {
    MaterialTheme {
        Navigator(listOf(BlankRoot, screen)) { navigator ->
            when (val top = navigator.lastItem) {
                BlankRoot -> Text("root")
                screen -> CurrentScreen()
                else -> Text("opened:${top::class.simpleName}")
            }
        }
    }
}

private object BlankRoot : Screen {
    @Composable
    override fun Content() = Unit
}
