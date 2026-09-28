package eu.kanade.tachiyomi.ui.reader

import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean

/** A fixed page layout chosen before the pager exists, and un-bookmarking from the chapter list. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class ReaderActivityEdgesTest {
    @get:Rule
    val compose = createComposeRule()

    private var harness = ReaderActivityHarness(pageCount = 2)

    @After
    fun tearDown() {
        harness.stop()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun start(mode: ReadingMode) {
        resetUiDispatcher()
        harness = ReaderActivityHarness(pageCount = 2, viewerFlags = mode.flagValue.toLong())
        harness.start()
        every { harness.vm.downloadManager.queueState } returns MutableStateFlow(emptyList())
    }

    @Test
    fun fixedLayoutSkipsDetection() {
        start(ReadingMode.LEFT_TO_RIGHT)
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.SINGLE_PAGE)
        val activity = harness.launch().get()
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<PagerViewer>()
        activity.viewModel.state.value.doublePages shouldBe false
    }

    @Test
    fun swipeUnbookmarksChapter() {
        start(ReadingMode.LEFT_TO_RIGHT)
        harness.vm.chapters(domainChapter(1L), domainChapter(2L), domainChapter(3L, bookmark = true))
        val cleared = AtomicBoolean()
        coEvery { harness.vm.updateChapter.await(match { it.id == 3L && it.bookmark == false }) } answers {
            cleared.set(true)
        }
        val activity = harness.launch().get()
        val settings = ReaderSettingsScreenModel(
            readerState = activity.viewModel.state,
            onChangeReadingMode = {},
            onChangeOrientation = {},
            preferences = harness.vm.readerPreferences,
        )
        val shown = activity.viewModel.state.value.copy(dialog = ReaderViewModel.Dialog.ChapterList)
        compose.setContent { MaterialTheme { activity.ReaderDialogs(shown, settings) } }
        compose.pollLabel("Chapter 3")
        compose.onAllNodesWithText("Chapter 3").onFirst().performTouchInput { swipeRight() }
        compose.waitUntil(timeoutMillis = 5_000) { cleared.get() }
    }
}
