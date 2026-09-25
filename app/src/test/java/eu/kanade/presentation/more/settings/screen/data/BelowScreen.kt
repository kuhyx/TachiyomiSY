package eu.kanade.presentation.more.settings.screen.data

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.ComposeContentTestRule
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry

/** The screen a data screen returns to when it pops itself. */
internal object BelowScreen : Screen {
    @Composable
    override fun Content() {
        Text("Below")
    }
}

/** Shows [screen] above [BelowScreen] in a real navigator, with [registry] answering its pickers. */
internal fun ComposeContentTestRule.showAbove(screen: Screen, registry: FakeResultRegistry = FakeResultRegistry()) {
    setContent {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry.owner()) {
            MaterialTheme { Navigator(listOf(BelowScreen, screen)) }
        }
    }
    waitForIdle()
}
