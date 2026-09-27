package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.DECODER_PACKAGE
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import java.util.concurrent.TimeUnit

/** Rotating, splitting and padding a page image before it is shown. */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = [DECODER_PACKAGE],
    shadows = [PagerShadowDecoder::class, PagerShadowDecoderCompanion::class],
)
internal class PagerPageProcessingTest {
    private val rig = PagerHolderRig()
    private val source = Buffer()
    private val result = Buffer()

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isWideImage(any()) } returns true
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
        every { ImageUtil.rotateImage(any(), any()) } returns result
        every { ImageUtil.splitInHalf(any(), any(), any()) } returns result
        every { ImageUtil.addHorizontalCenterMargin(any(), any(), any()) } returns result
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun rotationFollowsTheSetting() {
        every { rig.config.dualPageRotateToFit } returns true
        val holder = rig.holder()
        holder.process(holder.page, source) shouldBeSameInstanceAs result
        every { rig.config.dualPageRotateToFitInvert } returns true
        holder.process(holder.page, source)
        verify { ImageUtil.rotateImage(source, QUARTER_TURN_DEGREES) }
        verify { ImageUtil.rotateImage(source, -QUARTER_TURN_DEGREES) }
        every { ImageUtil.isWideImage(any()) } returns false
        holder.rotateDualPage(source) shouldBeSameInstanceAs source
    }

    @Test
    fun splitNeedsSettingAndWide() {
        val holder = rig.holder()
        holder.process(holder.page, source) shouldBeSameInstanceAs source
        every { rig.config.dualPageSplit } returns true
        holder.process(holder.page, source) shouldBeSameInstanceAs result
        verify { holder.viewer.onPageSplit(holder.page, any()) }
        every { ImageUtil.isWideImage(any()) } returns false
        holder.process(holder.page, source) shouldBeSameInstanceAs source
        holder.process(rig.insert(), source) shouldBeSameInstanceAs result
    }

    @Test
    fun halvesFollowReadingDirection() {
        rig.holder().splitInHalf(source)
        rig.holder(page = rig.insert()).splitInHalf(source)
        rig.holder(viewer = rig.viewer(rightToLeft = true)).splitInHalf(source)
        every { rig.config.dualPageInvert } returns true
        rig.holder().splitInHalf(source)
        rig.holder(page = rig.insert()).splitInHalf(source)
        every { rig.config.centerMarginType } returns PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN
        every { rig.config.doublePages } returns true
        rig.holder().splitInHalf(source)
        every { rig.config.imageCropBorders } returns true
        rig.holder().splitInHalf(source)
        verify { ImageUtil.splitInHalf(source, ImageUtil.Side.RIGHT, 0) }
        verify { ImageUtil.splitInHalf(source, ImageUtil.Side.LEFT, 0) }
        verify { ImageUtil.splitInHalf(source, any(), HALF_CENTER_MARGIN_PX) }
    }

    @Test
    fun wideImagesGetACenterMargin() {
        val holder = rig.holder()
        holder.handleWideImage(source) shouldBeSameInstanceAs source
        every { rig.config.centerMarginType } returns PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN
        holder.handleWideImage(source) shouldBeSameInstanceAs result
        every { ImageUtil.isAnimatedAndSupported(any()) } returns true
        holder.handleWideImage(source) shouldBeSameInstanceAs source
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
        every { ImageUtil.isWideImage(any()) } returns false
        holder.handleWideImage(source) shouldBeSameInstanceAs source
        every { rig.config.imageCropBorders } returns true
        holder.handleWideImage(source) shouldBeSameInstanceAs source
    }

    @Test
    fun centerMarginScalesWithHeight() {
        val holder = rig.holder()
        holder.calculateCenterMargin(10, 20) shouldBe 0
        every { rig.config.centerMarginType } returns PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN
        holder.calculateCenterMargin(10, 20) shouldBe CENTER_MARGIN_PX
        every { rig.config.imageCropBorders } returns true
        holder.calculateCenterMargin(10, 20) shouldBe 0
    }

    @Test
    fun imagesDecodeOrNot() {
        val holder = rig.holder()
        PagerDecodes.bitmaps += PagerDecodes.portrait()
        holder.decodeImage(rig.png(width = 2, height = 3)).shouldNotBeNull()
        holder.decodeImage(Buffer().writeUtf8("not an image")).shouldBeNull()
        PagerDecodes.throwNext = true
        holder.decodeImage(Buffer().writeUtf8("corrupt")).shouldBeNull()
    }

    @Test
    fun splittingDropsFullExtraPages() {
        val extra = rig.page(index = 1)
        val holder = rig.holder(extra = extra)
        holder.splitDoublePages()
        ShadowLooper.idleMainLooper(PAGE_SPLIT_DELAY_MS, TimeUnit.MILLISECONDS)
        holder.extraPage shouldBe extra
        extra.fullPage = true
        holder.splitDoublePages()
        ShadowLooper.idleMainLooper(PAGE_SPLIT_DELAY_MS, TimeUnit.MILLISECONDS)
        holder.extraPage.shouldBeNull()
        val full = rig.holder(extra = rig.page(index = 1)).also { it.page.fullPage = true }
        full.splitDoublePages()
        ShadowLooper.idleMainLooper(PAGE_SPLIT_DELAY_MS, TimeUnit.MILLISECONDS)
        full.extraPage.shouldBeNull()
    }
}
