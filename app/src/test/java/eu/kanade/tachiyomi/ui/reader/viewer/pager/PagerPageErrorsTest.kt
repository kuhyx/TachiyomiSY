package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.core.view.isVisible
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** The error layout a page holder shows when its image fails, with retry and web view actions. */
@RunWith(RobolectricTestRunner::class)
internal class PagerPageErrorsTest {
    private val rig = PagerHolderRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun errorShowsItsMessage() {
        val holder = rig.holder()
        holder.setError(IllegalStateException("boom"))
        val layout = holder.errorLayout!!
        layout.root.isVisible shouldBe true
        layout.errorMessage.text.toString() shouldContain "boom"
        layout.actionOpenInWebView.isVisible shouldBe false
        holder.showErrorLayout(null).errorMessage.text.toString() shouldBe "The image couldn't be loaded"
    }

    @Test
    fun spinnerHidesOnError() {
        val holder = rig.holder()
        holder.progressIndicator = ReaderProgressIndicator(rig.context)
        holder.setError(null)
        holder.errorLayout!!.root.isVisible shouldBe true
    }

    @Test
    fun retryAsksTheLoader() {
        val page = rig.page(withLoader = true)
        val holder = rig.holder(page)
        holder.showErrorLayout(null).actionRetry.performClick()
        verify { rig.loader.retryPage(page) }
        rig.holder().showErrorLayout(null).actionRetry.performClick()
    }

    @Test
    fun webPagesOpenInWebView() {
        val holder = rig.holder(rig.page(imageUrl = "https://img.example/1.jpg"))
        val layout = holder.showErrorLayout(null)
        layout.actionOpenInWebView.isVisible shouldBe true
        layout.actionOpenInWebView.performClick()
        val started = shadowOf(rig.context).nextStartedActivity
        started.component?.className shouldBe WebViewActivity::class.java.name
    }

    @Test
    fun localPagesHaveNoWebView() {
        val holder = rig.holder(rig.page(imageUrl = "content://pages/1"))
        val layout = holder.showErrorLayout(null)
        layout.actionOpenInWebView.isVisible shouldBe true
        layout.actionOpenInWebView.performClick()
        shadowOf(rig.context).nextStartedActivity.shouldBeNull()
    }

    @Test
    fun removingForgetsTheLayout() {
        val holder = rig.holder()
        val layout = holder.showErrorLayout(null)
        holder.removeErrorLayout()
        layout.root.isVisible shouldBe false
        holder.errorLayout.shouldBeNull()
        holder.removeErrorLayout()
    }
}
