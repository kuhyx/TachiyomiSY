package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
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

/** Joining a spread's two pages into one image, or keeping them apart when they cannot be joined. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class PagerPageMergingTest {
    private val rig = PagerHolderRig()
    private val merged = Buffer()
    private val first = Buffer().writeUtf8("first")
    private val second = Buffer().writeUtf8("second")

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
        every { ImageUtil.isWideImage(any()) } returns false
        every { ImageUtil.mergeBitmaps(any(), any(), any(), any(), any(), any()) } answers {
            lastArg<((Int) -> Unit)?>()?.invoke(PROGRESS_MERGING)
            merged
        }
    }

    @After
    fun tearDown() = rig.stop()

    private fun holder(viewer: PagerViewer = rig.viewer()) = rig.holder(extra = rig.page(index = 1), viewer = viewer)
        .also { it.progressIndicator = ReaderProgressIndicator(rig.context) }

    private fun decodes(vararg portrait: Boolean) {
        portrait.forEach { PagerDecodes.bitmaps += if (it) PagerDecodes.portrait() else PagerDecodes.landscape() }
    }

    @Test
    fun singlePageOnlyGetsAMargin() {
        rig.holder().mergePages(first, null) shouldBeSameInstanceAs first
    }

    @Test
    fun fullPagesStayAlone() {
        val holder = holder().also { it.page.fullPage = true }
        holder.mergePages(first, second) shouldBeSameInstanceAs first
    }

    @Test
    fun animatedFirstStaysAlone() {
        every { ImageUtil.isAnimatedAndSupported(first) } returns true
        val holder = holder()
        holder.mergePages(first, second) shouldBeSameInstanceAs first
        holder.page.fullPage shouldBe true
    }

    @Test
    fun animatedSecondStaysAlone() {
        every { ImageUtil.isAnimatedAndSupported(second) } returns true
        val holder = holder()
        holder.mergePages(first, second) shouldBeSameInstanceAs first
        holder.page.isolatedPage shouldBe true
        holder.extraPage?.fullPage shouldBe true
    }

    @Test
    fun undecodablePagesStayAlone() {
        val holder = holder()
        holder.mergePages(first, second) shouldBeSameInstanceAs first
        holder.page.fullPage shouldBe true
    }

    @Test
    fun landscapeFirstStaysAlone() {
        decodes(false)
        val holder = holder()
        holder.mergePages(first, second) shouldBeSameInstanceAs first
        holder.page.fullPage shouldBe true
    }

    @Test
    fun landscapeSecondStaysAlone() {
        decodes(true, false)
        val holder = holder()
        holder.mergePages(first, second) shouldBeSameInstanceAs first
        holder.page.isolatedPage shouldBe true
    }

    @Test
    fun portraitPagesJoin() {
        decodes(true, true, true, true, true, true)
        holder().mergePages(first, second) shouldBeSameInstanceAs merged
        ShadowLooper.idleMainLooper()
        verify { ImageUtil.mergeBitmaps(any(), any(), true, 0, any(), any()) }
        holder(rig.viewer(rightToLeft = true)).mergePages(first, second)
        verify { ImageUtil.mergeBitmaps(any(), any(), false, 0, any(), any()) }
        every { rig.config.invertDoublePages } returns true
        every { rig.config.centerMarginType } returns PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN
        holder(rig.viewer(rightToLeft = true)).mergePages(first, second)
        verify { ImageUtil.mergeBitmaps(any(), any(), true, any(), any(), any()) }
    }
}
