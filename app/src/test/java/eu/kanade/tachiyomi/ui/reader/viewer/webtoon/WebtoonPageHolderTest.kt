package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.widget.FrameLayout
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil

/** A webtoon page holder following its page through loading and showing, and being recycled. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonPageHolderTest {
    private val rig = WebtoonHolderRig()

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isWideImage(any()) } returns false
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
    }

    @After
    fun tearDown() = rig.stop()

    private fun loaded(page: ReaderPage) =
        eventually { runCatching { coVerify { rig.loader.loadPage(page) } }.isSuccess }

    @Test
    fun pagesWithoutLoaderWait() {
        val holder = rig.holder()
        holder.bind(rig.page())
        eventually { true }
        holder.progressContainer.isVisible shouldBe true
    }

    @Test
    fun statusesDriveTheSpinner() {
        val holder = rig.holder()
        val page = rig.page(withLoader = true)
        holder.bind(page)
        loaded(page)
        page.status = Page.State.Queue
        eventually { true }
        page.status = Page.State.LoadPage
        eventually { true }
        page.status = Page.State.DownloadImage
        page.progress = 30
        eventually { true }
        page.status = Page.State.Error(IllegalStateException("net"))
        eventually { holder.errorLayout != null }
    }

    @Test
    fun readyPageIsShown() {
        every { rig.config.imageCropBorders } returns true
        val holder = rig.holder()
        val page = rig.page(withLoader = true).also { it.stream = { Buffer().writeUtf8("img").inputStream() } }
        holder.bind(page)
        page.status = Page.State.Ready
        eventually { holder.frame.pageView != null }
    }

    @Test
    fun pagedAnimatedPageIsShown() {
        every { ImageUtil.isAnimatedAndSupported(any()) } returns true
        every { rig.config.continuousCropBorders } returns true
        val holder = rig.holder(rig.viewer(continuous = false))
        ((holder.frame.layoutParams as FrameLayout.LayoutParams).bottomMargin > 0) shouldBe true
        val page = rig.page(withLoader = true).also { it.stream = { Buffer().writeUtf8("gif").inputStream() } }
        holder.bind(page)
        page.status = Page.State.Ready
        eventually { holder.frame.pageView != null }
    }

    @Test
    fun streamlessOrBrokenPages() {
        val holder = rig.holder()
        val page = rig.page(withLoader = true)
        holder.bind(page)
        page.status = Page.State.Ready
        eventually { true }
        val broken = rig.page(withLoader = true).also { it.stream = { error("gone") } }
        holder.bind(broken)
        broken.status = Page.State.Ready
        eventually { holder.errorLayout != null }
    }

    @Test
    fun frameCallbacksReachTheHolder() {
        val holder = rig.holder()
        holder.frame.onImageLoadError?.invoke(IllegalStateException("decode"))
        (holder.errorLayout != null) shouldBe true
        holder.frame.onImageLoaded?.invoke()
        holder.progressContainer.isVisible shouldBe false
        holder.errorLayout shouldBe null
        holder.frame.onScaleChanged?.invoke(2f)
        verify { rig.activity.hideMenu() }
    }

    @Test
    fun recycleResets() {
        val holder = rig.holder()
        holder.bind(rig.page(withLoader = true))
        holder.initErrorLayout(null)
        holder.recycle()
        holder.errorLayout shouldBe null
        holder.progressContainer.isVisible shouldBe true
    }
}
