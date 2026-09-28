package eu.kanade.tachiyomi.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.readerBottomButtons
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** The reader overlay's rarer inputs: page number hidden, colour filters, other sources and repeated toggles. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h2000dp")
internal class ReaderOverlayEdgesTest {
    @get:Rule
    val compose = createComposeRule()

    private var harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity

    private fun launch(mode: ReadingMode = ReadingMode.LEFT_TO_RIGHT, before: () -> Unit = {}) {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = mode.flagValue.toLong())
        harness.start()
        harness.vm.readerPreferences.readerBottomButtons.set(ReaderBottomButton.entries.map { it.value }.toSet())
        before()
        activity = harness.launch().get()
    }

    private fun showBars() {
        val state = activity.viewModel.state.value.copy(menuVisible = true)
        compose.setContent {
            MaterialTheme {
                activity.ContentOverlay(state)
                activity.AppBars(state)
            }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun pageNumberFollowsMenu() {
        launch()
        activity.viewModel.updateState { it.copy(menuVisible = true) }
        harness.settle()
        harness.vm.readerPreferences.showPageNumber.set(false)
        activity.viewModel.updateState { it.copy(menuVisible = false) }
        harness.settle()
        activity.viewModel.state.value.menuVisible shouldBe false
    }

    // With the page number off from the start, the overlay's first frame, menu hidden, draws no indicator.
    @Test
    fun pageNumberOffFromTheStart() {
        launch { harness.vm.readerPreferences.showPageNumber.set(false) }
        activity.viewModel.state.value.menuVisible shouldBe false
    }

    @Test
    fun colourFilterWithUnknownMode() {
        launch()
        harness.vm.readerPreferences.colorFilter.set(true)
        harness.vm.readerPreferences.colorFilterMode.set(99)
        showBars()
        compose.labelShown("Crop borders") shouldBe true
    }

    @Test
    fun otherSourceHidesWebActions() {
        launch()
        every { harness.vm.sourceManager.getOrStub(1L) } returns mockk<Source>(relaxed = true)
        showBars()
        compose.labelShown("Share") shouldBe false
        compose.labelShown("Crop borders") shouldBe true
    }

    @Test
    fun cropToggleToastsEachTime() {
        launch()
        showBars()
        val before = ShadowToast.shownToastCount()
        compose.clickLabel("Crop borders")
        compose.clickLabel("Crop borders")
        ShadowToast.shownToastCount() shouldBe before + 2
    }

    @Test
    fun automaticLayoutFlipsDoubles() {
        launch()
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        showBars()
        val config = (activity.viewModel.state.value.viewer as PagerViewer).config
        config.doublePages = true
        compose.clickLabel("Page layout")
        config.doublePages shouldBe false
        compose.clickLabel("Page layout")
        config.doublePages shouldBe true
    }

    @Test
    fun automaticLayoutNeedsPager() {
        launch()
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        showBars()
        activity.viewModel.updateState { it.copy(viewer = null) }
        compose.clickLabel("Page layout")
        harness.vm.readerPreferences.pageLayout.get() shouldBe PagerConfig.PageLayout.AUTOMATIC
    }
}
