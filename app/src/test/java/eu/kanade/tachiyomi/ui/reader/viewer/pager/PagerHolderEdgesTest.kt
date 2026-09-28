package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import tachiyomi.core.common.util.system.ImageUtil
import java.util.concurrent.TimeUnit

/** A page holder's rarer paths: spinner present or not, spreads without a second stream, detaching twice. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class PagerHolderEdgesTest {
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
    fun queuedThenReadyResetsSpinner() {
        val page = rig.page(withLoader = true).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        val holder = rig.holder(page)
        page.status = Page.State.Queue
        eventually { holder.progressIndicator != null }
        page.status = Page.State.Ready
        eventually { holder.pageView != null }
    }

    @Test
    fun spreadWithoutSpinnerOrStream() {
        val extra = rig.page(index = 1)
        val page = rig.page(withLoader = true).also { it.stream = { rig.png(width = 2, height = 4).inputStream() } }
        val holder = rig.holder(page, extra = extra)
        page.status = Page.State.Ready
        eventually { holder.pageView != null }
        holder.progressIndicator.shouldBeNull()
    }

    @Test
    fun progressWithoutSpinner() {
        val holder = rig.holder()
        holder.updateProgress(40)
        holder.updateProgress(100)
        ShadowLooper.idleMainLooper()
        holder.progressIndicator.shouldBeNull()
    }

    @Test
    fun detachingTwiceIsQuiet() {
        val holder = rig.holder(rig.page(withLoader = true))
        rig.context.setContentView(holder)
        (holder.parent as ViewGroup).removeView(holder)
        rig.context.setContentView(holder)
        (holder.parent as ViewGroup).removeView(holder)
        holder.isAttachedToWindow shouldBe false
    }

    @Test
    fun errorWithoutUrlOrManga() {
        val holder = rig.holder(rig.page(imageUrl = "https://img.example/2.jpg"))
        every { rig.activity.viewModel.manga } returns null
        val layout = holder.showErrorLayout(null)
        layout.actionOpenInWebView.performClick()
        shadowOf(rig.context).nextStartedActivity.shouldNotBeNull()
        rig.holder().showErrorLayout(IllegalStateException("x")).actionOpenInWebView.isShown shouldBe false
    }

    @Test
    fun mergeProgressWithoutSpinner() {
        val holder = rig.holder(extra = rig.page(index = 1))
        PagerDecodes.bitmaps += listOf(PagerDecodes.portrait(), PagerDecodes.portrait())
        every { ImageUtil.mergeBitmaps(any(), any(), any(), any(), any(), any()) } returns Buffer()
        holder.mergePages(Buffer().writeUtf8("a"), Buffer().writeUtf8("b"))
        ShadowLooper.idleMainLooper()
        holder.progressIndicator.shouldBeNull()
    }

    @Test
    fun animatedSecondWithoutExtra() {
        val holder = rig.holder()
        every { ImageUtil.isAnimatedAndSupported(any()) } returnsMany listOf(false, true)
        holder.mergePages(Buffer().writeUtf8("a"), Buffer().writeUtf8("b"))
        holder.page.isolatedPage shouldBe true
        ShadowLooper.idleMainLooper(PAGE_SPLIT_DELAY_MS, TimeUnit.MILLISECONDS)
    }

    @Test
    fun spinnerFollowsMergeProgress() {
        val holder = rig.holder()
        holder.progressIndicator = ReaderProgressIndicator(rig.context)
        holder.updateProgress(PROGRESS_MERGING)
        ShadowLooper.idleMainLooper()
        holder.progressIndicator.shouldNotBeNull()
    }
}
