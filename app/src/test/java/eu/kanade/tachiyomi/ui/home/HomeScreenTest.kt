package eu.kanade.tachiyomi.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The phone layout: a bottom bar over the library, and the tab requests other screens send. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class HomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = HomeRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun barListsEveryTab() {
        rig.show()
        listOf("Updates", "History", "Browse", "More").forEach(rig::await)
    }

    @Test
    fun barHidesDisabledTabs() {
        rig.ui.showNavUpdates.set(false)
        rig.ui.showNavHistory.set(false)
        rig.ui.bottomBarLabels.set(false)
        rig.show()
        rig.await("Browse")
        compose.hasLabel("Updates") shouldBe false
        compose.hasLabel("History") shouldBe false
    }

    @Test
    fun badgesShowPendingCounts() {
        rig.library.harness.libraryPreferences.newUpdatesCount.set(4)
        rig.library.harness.sourcePreferences.extensionUpdatesCount.set(2)
        rig.show()
        rig.await("4")
        rig.await("2")
        rig.library.harness.libraryPreferences.newShowUpdatesCount.set(false)
        rig.awaitGone("4")
    }

    @Test
    fun bottomBarCanBeHidden() {
        rig.show()
        rig.send { HomeScreen.showBottomNav(false) }
        rig.awaitGone("Browse")
        rig.send { HomeScreen.showBottomNav(true) }
        rig.await("Browse")
    }

    @Test
    fun libraryRequestStaysHome() {
        rig.show()
        rig.tab("More")
        rig.await("Settings")
        rig.send { HomeScreen.openTab(HomeScreen.Tab.Library()) }
        rig.awaitGone("Settings")
    }

    @Test
    fun backReturnsToLibrary() {
        rig.show()
        rig.send { HomeScreen.openTab(HomeScreen.Tab.More(toDownloads = false)) }
        rig.await("Settings")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        rig.awaitGone("Settings")
    }

    @Test
    fun searchGoesToLibrary() {
        rig.show()
        rig.tab("More")
        rig.await("Settings")
        rig.send { HomeScreen.search("needle") }
        rig.await("needle")
    }

    @Test
    fun reselectingMoreOpensSettings() {
        rig.show()
        rig.tab("More")
        rig.await("Settings")
        rig.tab("More")
        rig.await("opened:SettingsScreen")
    }
}
