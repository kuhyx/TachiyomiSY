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
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Dialog
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
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

/** Bookmarking from the chapter list, repeated mode toasts and the settings dialog's menu hand-off. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class ReaderDialogsMoreTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = ReaderActivityHarness(pageCount = 2)
    private lateinit var activity: ReaderActivity
    private var shown by mutableStateOf(ReaderViewModel.State())

    @Before
    fun setUp() {
        resetUiDispatcher()
        harness.start()
        every { harness.vm.downloadManager.queueState } returns MutableStateFlow(emptyList())
        activity = harness.launch().get()
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(dialog: Dialog) {
        val settings = ReaderSettingsScreenModel(
            readerState = activity.viewModel.state,
            onChangeReadingMode = {},
            onChangeOrientation = {},
            preferences = harness.vm.readerPreferences,
        )
        shown = activity.viewModel.state.value.copy(dialog = dialog)
        compose.setContent { MaterialTheme { activity.ReaderDialogs(shown, settings) } }
        compose.waitForIdle()
    }

    @Test
    fun swipeBookmarksOneChapter() {
        show(Dialog.ChapterList)
        compose.pollLabel("Chapter 3")
        compose.onAllNodesWithText("Chapter 3").onFirst().performTouchInput { swipeRight() }
        compose.waitForIdle()
        coVerify(timeout = 5_000) { harness.vm.updateChapter.await(match { it.id == 3L && it.bookmark == true }) }
    }

    @Test
    fun secondToastReplacesFirst() {
        show(Dialog.OrientationModeSelect)
        compose.clickLabel("Portrait")
        compose.clickLabel("Apply")
        shown = shown.copy(dialog = null)
        compose.waitForIdle()
        shown = shown.copy(dialog = Dialog.OrientationModeSelect)
        compose.waitForIdle()
        compose.clickLabel("Landscape")
        compose.clickLabel("Apply")
        ShadowToast.getTextOfLatestToast() shouldBe "Landscape"
    }

    @Test
    fun filterTabHidesTheMenus() {
        activity.viewModel.showMenus(true)
        show(Dialog.Settings)
        compose.clickLabel("Custom filter")
        compose.waitUntil(timeoutMillis = 5_000) { !activity.viewModel.state.value.menuVisible }
        compose.clickLabel("General")
        compose.waitUntil(timeoutMillis = 5_000) { activity.viewModel.state.value.menuVisible }
    }
}
