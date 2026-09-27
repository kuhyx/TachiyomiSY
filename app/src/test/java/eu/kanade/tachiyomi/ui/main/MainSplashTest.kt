package eu.kanade.tachiyomi.ui.main

import android.view.View
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import eu.kanade.tachiyomi.R
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Before Android 12 the splash screen fades out with the app's own animation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
internal class MainSplashTest {
    private val rig = MainIntentRig()

    @Before
    fun setUp() {
        rig.start()
        rig.activity.setTheme(R.style.Theme_Tachiyomi)
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun exitAnimationRemovesSplash() {
        val activity = rig.activity
        val listener = slot<SplashScreen.OnExitAnimationListener>()
        val splash = mockk<SplashScreen> { every { setOnExitAnimationListener(capture(listener)) } just runs }
        activity.setSplashScreenExitAnimation(splash)
        val icon = View(activity).apply { translationY = 5f }
        val splashView = View(activity)
        val provider = mockk<SplashScreenViewProvider>(relaxed = true) {
            every { iconView } returns icon
            every { view } returns splashView
        }
        listener.captured.onSplashScreenExit(provider)
        icon.translationY shouldBe 0f
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        verify { provider.remove() }
        splashView.alpha shouldBe 0f
    }

    @Test
    fun noSplashNoAnimation() {
        val splash = mockk<SplashScreen>()
        rig.activity.setSplashScreenExitAnimation(null)
        verify(exactly = 0) { splash.setOnExitAnimationListener(any()) }
    }
}
