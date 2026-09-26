package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.LibraryTabRig
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class HomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val ui by lazy { UiPreferences(rig.harness.store) }

    @Before
    fun setUp() {
        rig.start()
        every { rig.harness.downloadManager.isDownloaderRunning } returns MutableStateFlow(false)
        every { rig.harness.downloadManager.queueState } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() = rig.stop()

    private fun showHome() {
        compose.setContent { ScreenHost(HomeScreen) }
        rig.waitFor("Alpha")
    }

    private fun send(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch { block() }
    }

    @Test
    fun tabsAndBadges() {
        rig.harness.libraryPreferences.newShowUpdatesCount.set(true)
        rig.harness.libraryPreferences.newUpdatesCount.set(3)
        rig.harness.sourcePreferences.extensionUpdatesCount.set(2)
        showHome()
        listOf("Library", "Updates", "History", "Browse", "More").forEach(rig::waitFor)
        rig.waitFor("3 new chapters")
        rig.waitFor("2 extension updates available")
    }

    @Test
    fun updateCountCanBeHidden() {
        rig.harness.libraryPreferences.newShowUpdatesCount.set(false)
        rig.harness.libraryPreferences.newUpdatesCount.set(3)
        showHome()
        rig.waitUntilGone("3 new chapters")
    }

    @Test
    @Config(qualifiers = "sw800dp-w1280dp-h800dp")
    fun tabletsUseARail() {
        showHome()
        rig.click("More")
        rig.waitFor("Download queue")
        rig.click("More")
        rig.waitFor("opened:SettingsScreen")
    }

    @Test
    fun hiddenTabsAndLabels() {
        ui.showNavUpdates.set(false)
        ui.showNavHistory.set(false)
        ui.bottomBarLabels.set(false)
        showHome()
        rig.waitUntilGone("Updates")
        rig.waitUntilGone("History")
    }

    @Test
    fun switchingTabsAndBack() {
        showHome()
        rig.click("More")
        rig.waitFor("Download queue")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        rig.waitFor("Alpha")
    }

    @Test
    fun reselectingATab() {
        showHome()
        rig.click("Library")
        rig.waitFor("Sort")
    }

    @Test
    fun searchingFromElsewhere() {
        showHome()
        rig.click("More")
        rig.waitFor("Download queue")
        send { HomeScreen.search("alp") }
        rig.waitFor("alp")
    }

    @Test
    fun openTabRequests() {
        showHome()
        // Regression test: the screen used to be pushed onto the tab navigator, which crashed.
        send { HomeScreen.openTab(HomeScreen.Tab.Library(mangaIdToOpen = 1L)) }
        rig.waitFor("opened:MangaScreen")
    }

    @Test
    fun bottomBarHides() {
        showHome()
        send { HomeScreen.showBottomNav(false) }
        rig.waitUntilGone("More")
        send { HomeScreen.showBottomNav(true) }
        rig.waitFor("More")
    }
}
