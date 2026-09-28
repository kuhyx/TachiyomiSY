package eu.kanade.tachiyomi.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Inside the main activity, a loaded history list tells the splash screen the app is ready. */
@RunWith(RobolectricTestRunner::class)
internal class HistoryTabReadyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = HistoryTabRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
        stopKoin()
    }

    @Test
    fun loadedListMarksReady() {
        // Only constructed: the tab reads it as its context, nothing runs its lifecycle.
        val main = Robolectric.buildActivity(MainActivity::class.java).get()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides main) { TabHost(HistoryTab) }
        }
        rig.waitFor("Title 1")
        compose.waitUntil(TAB_WAIT) { main.ready }
    }
}
