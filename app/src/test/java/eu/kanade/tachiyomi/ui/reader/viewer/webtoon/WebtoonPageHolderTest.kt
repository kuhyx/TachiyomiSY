package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.View
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.view.children
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.ReaderShadowDecoder
import eu.kanade.tachiyomi.ui.reader.ReaderShadowDecoderCompanion
import eu.kanade.tachiyomi.ui.reader.pngBytes
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.dualPageInvertWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFitInvertWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFitWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitWebtoon
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
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [ReaderShadowDecoder::class, ReaderShadowDecoderCompanion::class],
)
internal class WebtoonPageHolderTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 3, viewerFlags = ReadingMode.WEBTOON.flagValue.toLong())

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

    private fun holders(): List<WebtoonPageHolder> {
        val viewer = harness.launch().get().viewModel.state.value.viewer as WebtoonViewer
        harness.settle()
        return viewer.recycler.children.mapNotNull { viewer.recycler.getChildViewHolder(it) as? WebtoonPageHolder }
            .toList()
    }

    // Rebinds through the adapter: RecyclerView swaps in its own layout params only when it binds.
    private fun rebind(holder: WebtoonPageHolder) {
        holder.viewer.adapter.notifyItemChanged(holder.bindingAdapterPosition)
        harness.settle()
    }

    @Test
    fun statusDrivesTheHolder() {
        val holder = holders().first()
        val page = holder.page!!
        page.status = Page.State.LoadPage
        harness.settle()
        page.status = Page.State.DownloadImage
        page.progress = 40
        harness.settle()
        page.imageUrl = "https://img/0"
        page.status = Page.State.Error(IllegalStateException("boom"))
        harness.settle()
        val layout = holder.errorLayout.shouldNotBeNull()
        layout.actionRetry.performClick()
        harness.pageLoader.retried shouldBe listOf(page)
        layout.actionOpenInWebView.performClick()
        holder.initErrorLayout(null)
        page.imageUrl = "file:///x"
        holder.initErrorLayout(null)
        page.imageUrl = null
        holder.initErrorLayout(null)
        page.status = Page.State.Queue
        harness.settle()
        holder.errorLayout.shouldBeNull()
        holder.recycle()
    }

    @Test
    fun readyPageShowsImage() {
        harness.initialStatus = Page.State.Ready
        val holder = holders().first()
        harness.settleUntil { holder.frame.pageView != null }
        holder.frame.onImageLoaded()
        holder.progressContainer.visibility shouldBe View.GONE
        holder.frame.onScaleChanged(2f)
        holder.frame.onImageLoadError(null)
        holder.errorLayout.shouldNotBeNull()
    }

    @Test
    fun failuresAndMissingStreams() {
        harness.image = { throw IOException("gone") }
        harness.initialStatus = Page.State.Ready
        val holder = holders().first()
        harness.settleUntil { holder.errorLayout != null }
        holder.page!!.stream = null
        rebind(holder)
        harness.settle()
    }

    @Test
    fun wideImagesSplitOrRotate() {
        harness.vm.readerPreferences.dualPageSplitWebtoon.set(true)
        harness.image = { pngBytes(width = 12, height = 4) }
        harness.initialStatus = Page.State.Ready
        val holder = holders().first()
        harness.settleUntil { holder.frame.pageView != null }
        harness.vm.readerPreferences.dualPageInvertWebtoon.set(true)
        harness.settle()
        rebind(holder)
        harness.settle()
    }

    @Test
    fun rotateToFit() {
        harness.vm.readerPreferences.dualPageRotateToFitWebtoon.set(true)
        harness.vm.readerPreferences.dualPageRotateToFitInvertWebtoon.set(true)
        harness.image = { pngBytes(width = 12, height = 4) }
        harness.initialStatus = Page.State.Ready
        val holder = holders().first()
        harness.settleUntil { holder.frame.pageView != null }
        harness.vm.readerPreferences.dualPageRotateToFitInvertWebtoon.set(false)
        harness.settle()
        rebind(holder)
        harness.settle()
        harness.image = { pngBytes(width = 4, height = 12) }
        rebind(holder)
        harness.settle()
    }

    @Test
    fun noLoaderStaysIdle() {
        harness.withLoader = false
        holders().first().errorLayout.shouldBeNull()
    }
}
