package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabReadyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun loadedLibraryReadiesActivity() {
        // An attached but never created MainActivity: only its splash-screen flag is read.
        val main = Robolectric.buildActivity(MainActivity::class.java).get()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides main) { TabHost(LibraryTab) }
        }
        compose.waitUntil(WAIT) { main.ready }
    }

    @Test
    fun bottomBarFollowsTheSelection() {
        compose.setContent { TabHost(LibraryTab) }
        // Receiving the signal lets the effect that sent it finish.
        compose.waitUntil(WAIT) { HomeScreen.showBottomNavEvent.tryReceive().getOrNull() == true }
        compose.waitForIdle()
    }
}
