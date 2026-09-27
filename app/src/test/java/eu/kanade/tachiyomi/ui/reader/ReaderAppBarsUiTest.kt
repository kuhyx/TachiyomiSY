package eu.kanade.tachiyomi.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.readerBottomButtons
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The reader's app bars over a live activity: crop and page-layout buttons, the page slider, navigator sides. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h2000dp")
internal class ReaderAppBarsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var harness = ReaderActivityHarness(pageCount = 3)
    private lateinit var activity: ReaderActivity

    private fun launch(mode: ReadingMode? = null) {
        harness = ReaderActivityHarness(pageCount = 3, viewerFlags = mode?.flagValue?.toLong() ?: 0L)
        harness.start()
        harness.vm.readerPreferences.readerBottomButtons.set(ReaderBottomButton.entries.map { it.value }.toSet())
        activity = harness.launch().get()
    }

    // Only this composition is searched: the activity's own overlay lives in another window.
    private fun showBars() {
        val state = activity.viewModel.state.value.copy(menuVisible = true)
        compose.setContent {
            MaterialTheme {
                activity.ContentOverlay(state)
                activity.AppBars(state)
            }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun cropButtonToggles() {
        launch(ReadingMode.LEFT_TO_RIGHT)
        showBars()
        val before = harness.vm.readerPreferences.cropBorders.get()
        compose.clickLabel("Crop borders")
        harness.vm.readerPreferences.cropBorders.get() shouldBe !before
    }

    @Test
    fun pageLayoutCyclesOrDoubles() {
        launch(ReadingMode.LEFT_TO_RIGHT)
        val prefs = harness.vm.readerPreferences
        prefs.pageLayout.set(PagerConfig.PageLayout.SINGLE_PAGE)
        showBars()
        compose.clickLabel("Page layout")
        prefs.pageLayout.get() shouldBe PagerConfig.PageLayout.DOUBLE_PAGES
        prefs.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        harness.settle()
        val config = (activity.viewModel.state.value.viewer as PagerViewer).config
        val doubled = config.doublePages
        compose.clickLabel("Page layout")
        config.doublePages shouldBe !doubled
    }

    @Test
    fun sliderMovesThePage() {
        launch(ReadingMode.LEFT_TO_RIGHT)
        showBars()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        compose.waitForIdle()
        harness.settleUntil { activity.viewModel.state.value.currentPage == 2 }
        activity.isScrollingThroughPages shouldBe false
    }

    @Test
    fun webtoonCropAndLeftNavigator() {
        launch(ReadingMode.WEBTOON)
        harness.vm.readerPreferences.verticalNavigator.set(setOf(ReadingMode.WEBTOON))
        harness.vm.readerPreferences.verticalNavigatorOnLeft.set(true)
        harness.vm.readerPreferences.flashOnPageChange.set(true)
        showBars()
        compose.pollLabel("Crop borders")
    }

    @Test
    fun continuousCropRightNav() {
        launch(ReadingMode.CONTINUOUS_VERTICAL)
        harness.vm.readerPreferences.verticalNavigator.set(setOf(ReadingMode.CONTINUOUS_VERTICAL))
        showBars()
        compose.pollLabel("Crop borders")
    }

    @Test
    fun barsWaitForSources() {
        launch(ReadingMode.LEFT_TO_RIGHT)
        every { harness.vm.sourceManager.isInitialized } returns MutableStateFlow(false)
        showBars()
        compose.labelShown("Crop borders") shouldBe false
    }
}
