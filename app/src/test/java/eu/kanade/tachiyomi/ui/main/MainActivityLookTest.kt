package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.util.system.InternalResourceHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Dark system bars, the navigation bar scrim either way, and incognito turned off on the home screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp-night")
internal class MainActivityLookTest {
    private val rig = MainActivityRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun launchWithScrim(scrim: Boolean): MainActivity {
        mockkObject(InternalResourceHelper)
        every { InternalResourceHelper.getBoolean(any(), "config_navBarNeedsScrim", any()) } returns scrim
        val activity = rig.launch().get()
        rig.until { activity.navigator != null }
        rig.frames()
        return activity
    }

    @Test
    fun scrimDrawnWhenNeeded() {
        launchWithScrim(scrim = true).isFinishing shouldBe false
    }

    @Test
    fun noScrimWhenNotNeeded() {
        launchWithScrim(scrim = false).isFinishing shouldBe false
    }

    @Test
    fun incognitoOffStaysHome() {
        val activity = launchWithScrim(scrim = true)
        rig.harness.basePreferences.incognitoMode.set(true)
        rig.frames()
        rig.harness.basePreferences.incognitoMode.set(false)
        rig.frames()
        activity.navigator?.lastItem.shouldBeInstanceOf<HomeScreen>()
    }
}
