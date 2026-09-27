package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderActivityHarness
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The continuous vertical mode hands the "tapping by page" preference to the webtoon viewer it builds. */
@RunWith(RobolectricTestRunner::class)
internal class ReadingModeViewerTest {
    private val harness =
        ReaderActivityHarness(viewerFlags = ReadingMode.CONTINUOUS_VERTICAL.flagValue.toLong())
    private val tapping get() = harness.vm.readerPreferences.continuousVerticalTappingByPage

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun readerOpensTappingByPage() {
        tapping.set(true)
        val activity = harness.launch().get()
        val viewer = activity.viewModel.state.value.viewer as WebtoonViewer
        viewer.isContinuous shouldBe false
        viewer.tapByPage shouldBe true
    }

    @Test
    fun eachValueReachesTheViewer() {
        val activity = harness.launch().get()
        (activity.viewModel.state.value.viewer as WebtoonViewer).tapByPage shouldBe false
        tapping.set(true)
        val paged = ReadingMode.toViewer(ReadingMode.CONTINUOUS_VERTICAL.flagValue, activity) as WebtoonViewer
        paged.tapByPage shouldBe true
        tapping.set(false)
        val scrolled = ReadingMode.toViewer(ReadingMode.CONTINUOUS_VERTICAL.flagValue, activity) as WebtoonViewer
        scrolled.tapByPage shouldBe false
        // Plain webtoon never taps by page, whatever the continuous-vertical setting says.
        tapping.set(true)
        val webtoon = ReadingMode.toViewer(ReadingMode.WEBTOON.flagValue, activity) as WebtoonViewer
        webtoon.tapByPage shouldBe false
        listOf(paged, scrolled, webtoon).forEach { it.destroy() }
    }
}
