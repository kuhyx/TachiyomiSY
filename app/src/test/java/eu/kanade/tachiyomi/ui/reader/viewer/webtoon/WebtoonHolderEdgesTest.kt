package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.widget.LinearLayout
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.readerChapter
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
import org.robolectric.shadows.ShadowLooper
import tachiyomi.core.common.util.system.ImageUtil

/** Webtoon holders before binding, without a manga, and with every crop-borders combination. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonHolderEdgesTest {
    private val rig = WebtoonHolderRig()

    @Before
    fun setUp() {
        rig.start()
        every { ImageUtil.isAnimatedAndSupported(any()) } returns false
    }

    @After
    fun tearDown() = rig.stop()

    @Test
    fun unboundErrorHasNoPage() {
        val holder = rig.holder()
        val layout = holder.initErrorLayout(null)
        layout.actionOpenInWebView.isShown shouldBe false
        layout.actionRetry.performClick()
        layout.errorMessage.text.toString() shouldBe "The image couldn't be loaded"
    }

    @Test
    fun webViewWithoutManga() {
        every { rig.activity.viewModel.manga } returns null
        val holder = rig.holder()
        holder.bind(rig.page(imageUrl = "https://img.example/9.jpg"))
        holder.initErrorLayout(null).actionOpenInWebView.performClick()
        shadowOf(rig.context).nextStartedActivity.shouldNotBeNull()
    }

    @Test
    fun cropNeedsTheMatchingMode() {
        every { rig.config.imageCropBorders } returns true
        showReady(continuous = false)
        every { rig.config.imageCropBorders } returns false
        every { rig.config.continuousCropBorders } returns true
        showReady(continuous = true)
    }

    private fun showReady(continuous: Boolean) {
        val holder = rig.holder(rig.viewer(continuous = continuous))
        val page = rig.page(withLoader = true).also { it.stream = { Buffer().writeUtf8("img").inputStream() } }
        holder.bind(page)
        page.status = Page.State.Ready
        eventually { holder.frame.pageView != null }
    }

    // A page whose chapter has no loader (it was let go) is bound but never loaded.
    @Test
    fun pageWithoutLoaderIsLeftAlone() {
        val holder = rig.holder()
        holder.bind(rig.page())
        ShadowLooper.idleMainLooper()
        holder.frame.pageView shouldBe null
    }

    @Test
    fun unboundTransitionRecycles() {
        val holder = WebtoonTransitionHolder(LinearLayout(rig.context), rig.viewer())
        holder.recycle()
        holder.bind(ChapterTransition.Next(readerChapter(), null))
        holder.recycle()
        holder.itemView.isShown shouldBe false
    }
}
