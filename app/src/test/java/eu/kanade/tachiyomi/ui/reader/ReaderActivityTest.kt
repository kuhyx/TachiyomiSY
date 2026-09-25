package eu.kanade.tachiyomi.ui.reader

import android.content.Intent
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderActivityTest {

    private val harness = ReaderActivityHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun missingExtrasFinish() {
        val intent = Intent(harness.app, ReaderActivity::class.java)
        val activity = Robolectric.buildActivity(ReaderActivity::class.java, intent).setup().get()
        activity.isFinishing shouldBe true
    }

    @Test
    fun launchLoadsChapter() {
        val controller = harness.launch()
        val activity = controller.get()
        activity.isFinishing shouldBe false
        activity.viewModel.state.value.currentChapter!!.chapter.id shouldBe 2L
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<R2LPagerViewer>()
        controller.pause().stop().destroy()
    }
}
