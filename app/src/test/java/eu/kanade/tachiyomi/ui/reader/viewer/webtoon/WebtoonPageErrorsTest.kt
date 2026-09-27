package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.verify
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.util.system.ImageUtil

/** A webtoon page's error layout, and the rotate/split step before its image is shown. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonPageErrorsTest {
    private val rig = WebtoonHolderRig()
    private val source = Buffer()
    private val result = Buffer()

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isWideImage(any()) } returns true
        every { ImageUtil.rotateImage(any(), any()) } returns result
        every { ImageUtil.splitAndMerge(any(), any()) } returns result
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun errorHidesProgress() {
        val holder = rig.holder()
        holder.setError(IllegalStateException("boom"))
        holder.progressContainer.isVisible shouldBe false
        holder.errorLayout!!.errorMessage.text.toString() shouldContain "boom"
        holder.initErrorLayout(null).errorMessage.text.toString() shouldBe "The image couldn't be loaded"
        holder.errorLayout!!.actionRetry.performClick()
    }

    @Test
    fun retryAsksTheLoader() {
        val holder = rig.holder()
        val page = rig.page(withLoader = true)
        holder.page = page
        holder.initErrorLayout(null).actionRetry.performClick()
        verify { rig.loader.retryPage(page) }
        holder.page = rig.page()
        holder.errorLayout!!.actionRetry.performClick()
    }

    @Test
    fun webPagesOpenInWebView() {
        val holder = rig.holder()
        holder.page = rig.page(imageUrl = "https://img.example/1.jpg")
        val layout = holder.initErrorLayout(null)
        layout.actionOpenInWebView.isVisible shouldBe true
        layout.actionOpenInWebView.performClick()
        shadowOf(rig.context).nextStartedActivity.component?.className shouldBe WebViewActivity::class.java.name
        holder.page = rig.page(imageUrl = "content://1")
        holder.initErrorLayout(null).actionOpenInWebView.isVisible shouldBe true
    }

    @Test
    fun removingDropsTheLayout() {
        val holder = rig.holder()
        holder.removeErrorLayout()
        holder.initErrorLayout(null)
        holder.removeErrorLayout()
        holder.errorLayout.shouldBeNull()
    }

    @Test
    fun rotationFollowsTheSetting() {
        every { rig.config.dualPageRotateToFit } returns true
        val holder = rig.holder()
        holder.process(source) shouldBeSameInstanceAs result
        every { rig.config.dualPageRotateToFitInvert } returns true
        holder.process(source)
        every { ImageUtil.isWideImage(any()) } returns false
        holder.process(source) shouldBeSameInstanceAs source
        verify { ImageUtil.rotateImage(source, QUARTER_TURN_DEGREES) }
        verify { ImageUtil.rotateImage(source, -QUARTER_TURN_DEGREES) }
    }

    @Test
    fun widePagesSplitAndMerge() {
        val holder = rig.holder()
        holder.process(source) shouldBeSameInstanceAs source
        every { rig.config.dualPageSplit } returns true
        holder.process(source) shouldBeSameInstanceAs result
        every { rig.config.dualPageInvert } returns true
        holder.process(source)
        every { ImageUtil.isWideImage(any()) } returns false
        holder.process(source) shouldBeSameInstanceAs source
        verify { ImageUtil.splitAndMerge(source, ImageUtil.Side.RIGHT) }
        verify { ImageUtil.splitAndMerge(source, ImageUtil.Side.LEFT) }
    }
}
