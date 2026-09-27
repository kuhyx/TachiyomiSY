package eu.kanade.tachiyomi.ui.reader

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import android.view.WindowManager
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.util.concurrent.TimeUnit

/** The reader's window and container following each of its preferences. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderConfigTest {
    private var harness = ReaderActivityHarness(pageCount = 2)
    private val prefs get() = harness.vm.readerPreferences

    @After
    fun tearDown() = harness.stop()

    private fun launch(flags: Long = 0L): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 2, viewerFlags = flags)
        harness.start()
        return harness.launch().get()
    }

    private fun ReaderActivity.background(): Int = (binding.readerContainer.background as ColorDrawable).color

    @Test
    fun themePicksTheBackground() {
        val activity = launch()
        activity.background() shouldBe Color.BLACK
        prefs.readerTheme.set(0)
        harness.settle()
        activity.background() shouldBe Color.WHITE
        prefs.readerTheme.set(2)
        harness.settle()
        activity.background() shouldBe Color.rgb(0x20, 0x21, 0x25)
        prefs.readerTheme.set(3)
        harness.settle()
        activity.background() shouldBe Color.WHITE
    }

    @Test
    fun windowFlagsFollowPreferences() {
        val activity = launch()
        prefs.keepScreenOn.set(true)
        harness.settle()
        (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) shouldBe
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        prefs.keepScreenOn.set(false)
        harness.settle()
        (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) shouldBe 0
        prefs.fullscreen.set(false)
        prefs.drawUnderCutout.set(false)
        harness.settle()
    }

    @Test
    fun brightnessFollowsTheValue() {
        val activity = launch()
        prefs.customBrightness.set(true)
        listOf(50 to 0.5f, -10 to 0.01f).forEach { (value, brightness) ->
            prefs.customBrightnessValue.set(value)
            ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
            activity.window.attributes.screenBrightness shouldBe brightness
        }
        prefs.customBrightness.set(false)
        harness.settle()
        activity.window.attributes.screenBrightness shouldBe WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.viewModel.state.value.brightnessOverlayValue shouldBe 0
    }

    @Test
    fun colorFiltersPaintTheLayer() {
        val activity = launch()
        prefs.grayscale.set(true)
        harness.settle()
        activity.binding.viewerContainer.layerType shouldBe View.LAYER_TYPE_HARDWARE
        prefs.invertedColors.set(true)
        prefs.grayscale.set(false)
        harness.settle()
        prefs.invertedColors.set(false)
        harness.settle()
    }

    @Test
    fun displayProfileIsLoaded() {
        launch()
        val profile = File.createTempFile("profile", ".icc").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        harness.vm.basePreferences.displayProfile.set(Uri.fromFile(profile).toString())
        harness.settle()
        harness.vm.basePreferences.displayProfile.set(Uri.fromFile(File(profile.path + ".missing")).toString())
        harness.settle()
        profile.exists() shouldBe true
    }

    @Test
    fun layoutChangesReachThePager() {
        val activity = launch()
        prefs.pageLayout.set(PagerConfig.PageLayout.DOUBLE_PAGES)
        harness.settle()
        prefs.dualPageSplitPaged.set(true)
        harness.settle()
        prefs.dualPageSplitPaged.set(false)
        harness.settle()
        prefs.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        prefs.dualPageSplitPaged.set(true)
        prefs.dualPageSplitPaged.set(false)
        harness.settle()
        prefs.pageLayout.set(PagerConfig.PageLayout.SINGLE_PAGE)
        prefs.dualPageSplitPaged.set(true)
        prefs.dualPageSplitPaged.set(false)
        harness.settle()
        activity.isFinishing shouldBe false
    }

    @Test
    fun webtoonIgnoresPagerLayout() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong())
        prefs.pageLayout.set(PagerConfig.PageLayout.DOUBLE_PAGES)
        prefs.dualPageSplitPaged.set(true)
        harness.settle()
        activity.viewModel.state.value.doublePages shouldBe false
    }
}
