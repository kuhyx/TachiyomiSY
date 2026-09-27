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

/** The tablet layout: the same tabs on a navigation rail. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw800dp-w1280dp-h2000dp")
internal class HomeTabletTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = HomeRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun railListsEnabledTabs() {
        rig.ui.showNavHistory.set(false)
        rig.show()
        rig.await("Updates")
        compose.hasLabel("History") shouldBe false
    }

    @Test
    fun railSwitchesAndReselects() {
        rig.show()
        rig.tab("More")
        rig.await("Settings")
        rig.tab("More")
        rig.await("opened:SettingsScreen")
    }
}
