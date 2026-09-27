package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import tachiyomi.core.common.util.system.ImageUtil

/** A page holder following its page through loading, downloading, showing and failing. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class PagerPageHolderTest {
    private val rig = PagerHolderRig()

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
        every { ImageUtil.isWideImage(any()) } returns false
        every { ImageUtil.chooseBackground(any(), any()) } returns ColorDrawable()
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun pageWithoutLoaderWaits() {
        val holder = rig.holder()
        eventually { true }
        holder.progressIndicator.shouldBeNull()
        holder.item shouldBe (holder.page to null)
    }

    @Test
    fun statusesDriveTheSpinner() {
        val page = rig.page(withLoader = true)
        val holder = rig.holder(page)
        eventually { runCatching { coVerify { rig.loader.loadPage(page) } }.isSuccess }
        page.status = Page.State.Queue
        eventually { holder.progressIndicator != null }
        page.status = Page.State.LoadPage
        eventually { true }
        page.status = Page.State.DownloadImage
        page.progress = 40
        eventually { true }
        page.status = Page.State.Error(IllegalStateException("net"))
        eventually { holder.errorLayout != null }
        holder.errorLayout!!.errorMessage.text.toString() shouldContain "net"
    }

    @Test
    fun readyPageIsShown() {
        every { rig.config.automaticBackground } returns true
        val page = rig.page(withLoader = true).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        val holder = rig.holder(page)
        page.status = Page.State.Ready
        eventually { holder.pageView != null || holder.errorLayout != null }
        holder.errorLayout?.errorMessage?.text.shouldBeNull()
        holder.pageBackground.shouldNotBeNull()
    }

    @Test
    fun readySpreadIsMerged() {
        every { rig.config.automaticBackground } returns false
        val extra = rig.page(index = 1).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        val page = rig.page(withLoader = true).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        PagerDecodes.bitmaps += listOf(PagerDecodes.portrait(), PagerDecodes.portrait())
        every { ImageUtil.mergeBitmaps(any(), any(), any(), any(), any(), any()) } returns Buffer()
        val holder = rig.holder(page, extra = extra)
        holder.progressIndicator = ReaderProgressIndicator(rig.context)
        page.status = Page.State.Ready
        eventually { holder.pageView != null }
    }

    @Test
    fun splitSettingProcessesThePage() {
        every { rig.config.dualPageSplit } returns true
        every { ImageUtil.isAnimatedAndSupported(any()) } returns true
        val page = rig.page(withLoader = true).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        val holder = rig.holder(page)
        page.status = Page.State.Ready
        eventually { holder.pageView != null }
    }

    @Test
    fun streamlessOrBrokenPages() {
        val page = rig.page(withLoader = true)
        rig.holder(page)
        page.status = Page.State.Ready
        eventually { true }
        val broken = rig.page(withLoader = true).also { it.stream = { error("gone") } }
        val holder = rig.holder(broken)
        broken.status = Page.State.Ready
        eventually { holder.errorLayout != null }
    }

    @Test
    fun extraPageLoadsToo() {
        val extra = rig.page(index = 1, withLoader = true)
        rig.holder(rig.page(), extra = extra)
        eventually { runCatching { coVerify { rig.loader.loadPage(extra) } }.isSuccess }
    }

    @Test
    fun progressAndCallbacks() {
        val holder = rig.holder()
        holder.progressIndicator = ReaderProgressIndicator(rig.context)
        holder.updateProgress(50)
        ShadowLooper.idleMainLooper()
        holder.updateProgress(100)
        ShadowLooper.idleMainLooper()
        holder.onImageLoaded()
        holder.onImageLoadError(IllegalStateException("decode"))
        holder.errorLayout.shouldNotBeNull()
        holder.onScaleChanged(2f)
        verify { rig.activity.hideMenu() }
        rig.holder().onImageLoaded()
    }

    @Test
    fun detachStopsLoading() {
        val holder = rig.holder(rig.page(withLoader = true), extra = rig.page(index = 1, withLoader = true))
        rig.context.setContentView(holder)
        (holder.parent as ViewGroup).removeView(holder)
        holder.isAttachedToWindow shouldBe false
    }
}
