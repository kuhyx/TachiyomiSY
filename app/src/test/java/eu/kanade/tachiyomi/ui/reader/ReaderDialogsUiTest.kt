package eu.kanade.tachiyomi.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** Each dialog the reader view model can ask for, composed over a live reader activity. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class ReaderDialogsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 2)
    private lateinit var activity: ReaderActivity
    private val modes = mutableListOf<ReadingMode>()
    private var shown by mutableStateOf(ReaderViewModel.State())

    @Before
    fun setUp() {
        harness.start()
        every { harness.vm.downloadManager.queueState } returns MutableStateFlow(emptyList())
        activity = harness.launch().get()
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(dialog: Dialog) {
        val settings = ReaderSettingsScreenModel(
            readerState = activity.viewModel.state,
            onChangeReadingMode = { modes += it },
            onChangeOrientation = {},
            preferences = harness.vm.readerPreferences,
        )
        // Only this composition shows the dialog: the activity's own overlay would show a second copy.
        shown = activity.viewModel.state.value.copy(dialog = dialog)
        compose.setContent { MaterialTheme { activity.ReaderDialogs(shown, settings) } }
        compose.waitForIdle()
    }

    @Test
    fun loadingShowsProgress() {
        show(Dialog.Loading)
        compose.pollLabel("Loading…")
    }

    @Test
    fun helpDialogsClose() {
        show(Dialog.AutoScrollHelp)
        compose.pollLabel("Autoscroll help")
        shown = shown.copy(dialog = Dialog.BoostPageHelp)
        compose.pollLabel("Boost page help")
        shown = shown.copy(dialog = Dialog.RetryAllHelp)
        compose.pollLabel("Retry all help")
        compose.clickLabel("OK")
        activity.viewModel.state.value.dialog shouldBe null
    }

    @Test
    fun readingModeToastsUnlessShown() {
        show(Dialog.ReadingModeSelect)
        compose.clickLabel("Paged (right to left)")
        compose.clickLabel("Apply")
        modes shouldBe listOf(ReadingMode.RIGHT_TO_LEFT)
        ShadowToast.getTextOfLatestToast() shouldBe "Paged (right to left)"
    }

    @Test
    fun readingModeOverlayMeansNoToast() {
        harness.vm.readerPreferences.showReadingMode.set(true)
        ShadowToast.reset()
        show(Dialog.ReadingModeSelect)
        compose.clickLabel("Paged (right to left)")
        compose.clickLabel("Apply")
        ShadowToast.shownToastCount() shouldBe 0
    }

    @Test
    fun orientationAlwaysToasts() {
        show(Dialog.OrientationModeSelect)
        compose.clickLabel("Portrait")
        compose.clickLabel("Apply")
        ShadowToast.getTextOfLatestToast() shouldBe "Portrait"
    }

    @Test
    fun pageActionsReachImages() {
        show(Dialog.PageActions(ReaderPage(0)))
        compose.clickLabel("Share")
        compose.clickLabel("Save")
        compose.clickLabel("Set as cover")
        compose.clickLabel("OK")
        compose.labelShown("Set as cover") shouldBe true
    }

    @Test
    fun spreadActionsReachImages() {
        show(Dialog.PageActions(ReaderPage(0), extraPage = ReaderPage(1)))
        compose.clickLabel("Share first page")
        compose.clickLabel("Save combined page")
        compose.labelShown("Share first page") shouldBe true
    }

    @Test
    fun settingsToggleMenus() {
        show(Dialog.Settings)
        compose.waitForIdle()
        activity.viewModel.state.value.dialog shouldBe null
    }

    @Test
    fun chapterListLoadsAChapter() {
        show(Dialog.ChapterList)
        compose.pollLabel("Chapter 1")
        compose.onAllNodesWithText("Chapter 1").onFirst().performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.clickLabel("Chapter 1")
        activity.viewModel.state.value.dialog shouldBe null
        // The chapter loads on IO and lands on the main looper; let it finish before Koin stops.
        harness.settleUntil { activity.viewModel.state.value.viewerChapters?.currChapter?.chapter?.id == 1L }
    }
}
