package eu.kanade.tachiyomi.ui.main

import android.os.Bundle
import android.os.Looper
import eu.kanade.tachiyomi.extension.api.ExtensionApi
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.more.OnboardingScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockkConstructor
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

/** The extension update check on start, and a configuration change that skips the launch-only work. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MainActivityRecreateTest {
    private val rig = MainActivityRig()
    private val checked = AtomicBoolean(false)

    @Before
    fun setUp() {
        rig.start()
        mockkConstructor(ExtensionApi::class)
        coEvery { anyConstructed<ExtensionApi>().checkForUpdates(any(), any()) } answers {
            checked.set(true)
            null
        }
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun extensionUpdatesAreChecked() {
        val activity = rig.launch().get()
        rig.until { activity.navigator != null && checked.get() }
    }

    // A restored activity (saved state given) skips the launch-only work: splash, migrations, analytics.
    @Test
    fun restoredActivityKeepsHome() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        shadowOf(controller.get()).setIsTaskRoot(true)
        try {
            val activity = controller.create(Bundle()).start().postCreate(Bundle()).resume().visible().get()
            rig.until { activity.navigator != null }
            activity.navigator?.lastItem.shouldBeInstanceOf<HomeScreen>()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    // Brought back with the onboarding on top, the activity does not push a second one.
    @Test
    fun restoredOnboardingIsKept() {
        rig.harness.basePreferences.shownOnboardingFlow.set(false)
        val first = Robolectric.buildActivity(MainActivity::class.java)
        val second = Robolectric.buildActivity(MainActivity::class.java)
        shadowOf(first.get()).setIsTaskRoot(true)
        shadowOf(second.get()).setIsTaskRoot(true)
        val saved = Bundle()
        var firstLive = false
        var secondLive = false
        try {
            first.setup()
            firstLive = true
            rig.until { first.get().navigator?.lastItem is OnboardingScreen }
            // Torn down as a configuration change does, its state saved on the way.
            first.pause().saveInstanceState(saved).stop().destroy()
            firstLive = false
            val activity = second.create(saved).start().postCreate(saved).resume().visible().get()
            secondLive = true
            rig.until { activity.navigator != null }
            rig.frames()
            activity.navigator?.items?.count { it is OnboardingScreen } shouldBe 1
        } finally {
            if (firstLive) first.pause().stop().destroy()
            if (secondLive) second.pause().stop().destroy()
            shadowOf(Looper.getMainLooper()).idle()
        }
    }
}
