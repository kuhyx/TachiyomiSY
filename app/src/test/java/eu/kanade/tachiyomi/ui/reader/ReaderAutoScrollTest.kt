package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.autoscrollInterval
import eu.kanade.tachiyomi.ui.reader.setting.smoothAutoScroll
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** Auto-scroll turns pages while the menu is hidden, and waits while it is shown. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderAutoScrollTest {
    private var harness = ReaderActivityHarness(pageCount = 4)

    @After
    fun tearDown() = harness.stop()

    private fun launch(flags: Long = 0L): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 4, viewerFlags = flags)
        harness.start()
        harness.vm.readerPreferences.autoscrollInterval.set(1f)
        return harness.launch().get()
    }

    private fun scrolled(activity: ReaderActivity, enabled: Boolean) {
        activity.viewModel.toggleAutoScroll(enabled)
        ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)
    }

    @Test
    fun pagerTurnsWhileMenuHidden() {
        val activity = launch()
        activity.viewModel.updateState { it.copy(menuVisible = true) }
        val viewer = activity.viewModel.state.value.viewer as PagerViewer
        val start = viewer.pager.currentItem
        scrolled(activity, enabled = true)
        viewer.pager.currentItem shouldBe start
        activity.viewModel.updateState { it.copy(menuVisible = false) }
        ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)
        viewer.pager.currentItem shouldNotBe start
        scrolled(activity, enabled = false)
    }

    @Test
    fun otherViewersIdle() {
        val activity = launch()
        activity.viewModel.updateState { it.copy(menuVisible = false, viewer = null) }
        scrolled(activity, enabled = true)
        scrolled(activity, enabled = false)
        activity.viewModel.state.value.autoScroll shouldBe false
    }

    @Test
    fun webtoonScrollsSmoothOrStepped() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong())
        activity.viewModel.updateState { it.copy(menuVisible = false) }
        harness.vm.readerPreferences.smoothAutoScroll.set(true)
        scrolled(activity, enabled = true)
        harness.vm.readerPreferences.smoothAutoScroll.set(false)
        ShadowLooper.idleMainLooper(3, TimeUnit.SECONDS)
        scrolled(activity, enabled = false)
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<WebtoonViewer>()
    }

    @Test
    fun webtoonStepsWhenNotSmooth() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong())
        harness.vm.readerPreferences.smoothAutoScroll.set(false)
        activity.viewModel.updateState { it.copy(menuVisible = false) }
        scrolled(activity, enabled = true)
        scrolled(activity, enabled = false)
        activity.viewModel.state.value.autoScroll shouldBe false
    }
}
