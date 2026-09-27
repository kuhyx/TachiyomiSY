package eu.kanade.tachiyomi.ui.main

import android.os.Bundle
import eu.kanade.tachiyomi.extension.api.ExtensionApi
import eu.kanade.tachiyomi.ui.home.HomeScreen
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
}
