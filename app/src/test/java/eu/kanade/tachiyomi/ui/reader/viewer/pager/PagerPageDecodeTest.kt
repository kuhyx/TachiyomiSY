package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.view.children
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.ReaderShadowDecoder
import eu.kanade.tachiyomi.ui.reader.ReaderShadowDecoderCompanion
import eu.kanade.tachiyomi.ui.reader.pngBytes
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ReaderShadowDecoder::class, ReaderShadowDecoderCompanion::class],
)
internal class PagerPageDecodeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 4)

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
        harness.withLoader = true
        harness.image = { pngBytes(width = 4, height = 6) }
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun launch(): Pair<ActivityController<ReaderActivity>, List<PagerPageHolder>> {
        val controller = harness.launch()
        harness.settle()
        val viewer = controller.get().viewModel.state.value.viewer as PagerViewer
        return controller to viewer.pager.children.filterIsInstance<PagerPageHolder>().toList()
    }

    // The holders a config change rebuilt, rather than the ones the launch saw.
    private fun live(viewer: PagerViewer): List<PagerPageHolder> =
        viewer.pager.children.filterIsInstance<PagerPageHolder>().toList()

    @Test
    fun readyPagesDecodeWithoutSpinner() {
        harness.initialStatus = Page.State.Ready
        val (controller, holders) = launch()
        harness.settleUntil { holders.all { it.pageView != null } }
        holders.first().progressIndicator.shouldBeNull()
        holders.first().updateProgress(100)
        holders.first().updateProgress(30)
        holders.first().onImageLoaded()
        harness.settle()
        controller.pause().stop().destroy()
    }

    @Test
    fun failingStreamShowsError() {
        harness.image = { throw IOException("gone") }
        harness.initialStatus = Page.State.Ready
        val (_, holders) = launch()
        harness.settleUntil { holders.first().errorLayout != null }
        holders.first().errorLayout!!.errorMessage.text.toString() shouldBe "gone"
    }

    @Test
    fun noLoaderNoWork() {
        harness.withLoader = false
        val (_, holders) = launch()
        holders.first().progressIndicator.shouldBeNull()
    }

    @Test
    fun doublePagesMerge() {
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.DOUBLE_PAGES)
        harness.initialStatus = Page.State.Ready
        val (_, holders) = launch()
        val viewer = holders.first().viewer
        harness.settle()
        viewer.pager.setCurrentItem(viewer.adapter.joinedItems.indexOfFirst { it.second != null }, false)
        harness.settle()
        val paired = live(viewer).first { it.extraPage != null }
        harness.settleUntil { paired.pageView != null }
        paired.progressIndicator.shouldBeNull()
    }

    @Test
    fun wideSpreadSplits() {
        harness.vm.readerPreferences.dualPageSplitPaged.set(true)
        harness.image = { pngBytes(width = 12, height = 4) }
        harness.initialStatus = Page.State.Ready
        val (_, holders) = launch()
        harness.settleUntil { holders.first().pageView != null }
        holders.first().page.shiftedPage shouldBe false
    }

    @Test
    fun automaticThemePicksBackground() {
        harness.vm.readerPreferences.readerTheme.set(3)
        harness.image = { pngBytes(width = 40, height = 60) }
        harness.initialStatus = Page.State.Ready
        val (_, holders) = launch()
        val viewer = holders.first().viewer
        harness.settleUntil { live(viewer).any { it.pageBackground != null } }
        live(viewer).first { it.pageBackground != null }.pageBackground.shouldNotBeNull()
    }
}
