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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ReaderShadowDecoder::class, ReaderShadowDecoderCompanion::class],
)
internal class PagerPageHolderTest {

    // Holds Compose on a test clock: the page spinner animates forever, which a free-running
    // clock turns into an endless looper.
    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity
    private lateinit var viewer: PagerViewer

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
        harness.withLoader = true
        harness.image = { pngBytes(width = 4, height = 6) }
        activity = harness.launch().get()
        viewer = activity.viewModel.state.value.viewer as PagerViewer
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun holders(): List<PagerPageHolder> = viewer.pager.children.filterIsInstance<PagerPageHolder>().toList()

    private fun currentHolder(): PagerPageHolder {
        harness.settle()
        return holders().first { it.page == activity.viewModel.state.value.currentChapter!!.pages!![0] }
    }

    @Test
    fun statusDrivesTheHolder() {
        val holder = currentHolder()
        val page = holder.page
        holder.progressIndicator.shouldNotBeNull()
        page.status = Page.State.LoadPage
        harness.settle()
        page.status = Page.State.DownloadImage
        page.progress = 40
        harness.settle()
        page.status = Page.State.Error(IllegalStateException("boom"))
        harness.settle()
        holder.errorLayout.shouldNotBeNull()
        holder.errorLayout!!.errorMessage.text.toString() shouldBe "IllegalStateException: boom"
        page.status = Page.State.Queue
        harness.settle()
        holder.errorLayout.shouldBeNull()
    }

    @Test
    fun readyPageShowsImage() {
        val holder = currentHolder()
        holder.page.status = Page.State.Ready
        harness.settleUntil { holder.pageView != null }
        holder.onImageLoaded()
        holder.onScaleChanged(2f)
        holder.onImageLoadError(null)
        holder.errorLayout!!.errorMessage.text.isNotEmpty() shouldBe true
        holder.updateProgress(50)
        holder.updateProgress(100)
        harness.settle()
    }

    @Test
    fun errorLayoutActions() {
        val holder = currentHolder()
        holder.page.imageUrl = "https://img/0"
        val layout = holder.showErrorLayout(IllegalStateException("x"))
        layout.actionRetry.performClick()
        harness.pageLoader.retried shouldBe listOf(holder.page)
        layout.actionOpenInWebView.performClick()
        holder.page.imageUrl = "file:///x"
        holder.showErrorLayout(null)
        holder.page.imageUrl = null
        holder.showErrorLayout(null)
        holder.removeErrorLayout()
        holder.removeErrorLayout()
    }

    @Test
    fun undecodableImageShowsError() {
        harness.image = { byteArrayOf(1, 2, 3) }
        val holder = currentHolder()
        holder.page.status = Page.State.Ready
        harness.settleUntil { holder.errorLayout != null }
    }

    @Test
    fun streamlessPageStaysBlank() {
        val holder = currentHolder()
        holder.page.stream = null
        holder.page.status = Page.State.Ready
        harness.settle()
        holder.pageView.shouldBeNull()
    }
}
