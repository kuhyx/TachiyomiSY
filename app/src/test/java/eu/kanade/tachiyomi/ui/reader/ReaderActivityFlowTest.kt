package eu.kanade.tachiyomi.ui.reader

import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SaveImageResult
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ReaderActivityFlowTest {

    // Compose runs on a test clock: an animating page spinner never lets an auto-advancing clock idle.
    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness()

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun ReaderActivity.nextStarted(): Intent? = shadowOf(this).nextStartedActivity

    @Test
    fun pageSelectionTexts() {
        val activity = harness.launch().get()
        val page = activity.viewModel.state.value.currentChapter!!.pages!![1]
        activity.onPageSelected(page)
        activity.viewModel.state.value.currentPageText shouldBe "2"
        activity.onPageSelected(page, hasExtraPage = true)
        activity.viewModel.state.value.currentPageText shouldBe "2-3"
        activity.onPageLongTap(page)
        activity.viewModel.state.value.dialog shouldBe ReaderViewModel.Dialog.PageActions(page)
        activity.onPageLongTap(page, page)
    }

    @Test
    fun moveToPageNeedsEverything() {
        val activity = harness.launch().get()
        activity.moveToPageIndex(2)
        activity.moveToPageIndex(99)
        activity.viewModel.onViewerLoaded(null)
        activity.moveToPageIndex(0)
        activity.setProgressDialog(true)
        activity.viewModel.state.value.dialog shouldBe ReaderViewModel.Dialog.Loading
        activity.setProgressDialog(false)
        activity.viewModel.state.value.dialog shouldBe null
    }

    @Test
    fun navigationStartsActivities() {
        val activity = harness.launch().get()
        activity.openMangaScreen()
        activity.nextStarted().shouldNotBeNull()
        activity.assistUrl = null
        activity.openChapterInWebView()
        activity.openChapterInBrowser()
        activity.shareChapter()
        activity.nextStarted() shouldBe null
        activity.assistUrl = "https://example.org/c/2"
        activity.openChapterInWebView()
        activity.nextStarted().shouldNotBeNull()
        activity.shareChapter()
        activity.nextStarted().shouldNotBeNull()
        activity.openChapterInBrowser()
    }

    @Test
    fun webViewNeedsMangaAndSource() {
        val activity = harness.launch().get()
        activity.assistUrl = "https://example.org/c/2"
        every { harness.vm.sourceManager.getOrStub(1L) } returns mockk<Source>()
        activity.openChapterInWebView()
        activity.viewModel.updateState { it.copy(manga = null) }
        activity.openChapterInWebView()
        activity.openMangaScreen()
        activity.nextStarted() shouldBe null
    }

    @Test
    fun orientationAndModeToast() {
        val activity = harness.launch().get()
        activity.setOrientation(ReaderOrientation.LOCKED_PORTRAIT.flagValue)
        activity.requestedOrientation shouldBe ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity.setOrientation(ReaderOrientation.LOCKED_PORTRAIT.flagValue)
        activity.showReadingModeToast(ReadingMode.WEBTOON.flagValue)
        activity.showReadingModeToast(ReadingMode.VERTICAL.flagValue)
    }

    @Test
    fun resultsShowFeedback() {
        val activity = harness.launch().get()
        val uri = Uri.parse("content://reader/1")
        activity.onSaveImageResult(SaveImageResult.Success(uri))
        activity.onSaveImageResult(SaveImageResult.Error(IllegalStateException("x")))
        SetAsCoverResult.entries.forEach { activity.onSetAsCoverResult(it) }
        activity.onCopyImageResult(uri)
        val clipboard = activity.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip!!.getItemAt(0).uri shouldBe uri
    }

    @Test
    fun shareTextsForSpreads() {
        val activity = harness.launch().get()
        val pages = activity.viewModel.state.value.currentChapter!!.pages!!
        val uri = Uri.parse("content://reader/1")
        activity.onShareImageResult(uri, pages[0])
        activity.onShareImageResult(uri, pages[0], pages[1])
        activity.nextStarted().shouldNotBeNull()
        activity.viewModel.updateState { it.copy(manga = null) }
        activity.onShareImageResult(uri, pages[0])
    }

    @Test
    fun eventsReachTheActivity() {
        val activity = harness.launch().get()
        val uri = Uri.parse("content://reader/1")
        val page = activity.viewModel.state.value.currentChapter!!.pages!![0]
        val events = listOf(
            Event.ReloadViewerChapters,
            Event.PageChanged,
            Event.SetOrientation(ReaderOrientation.LOCKED_LANDSCAPE.flagValue),
            Event.SavedImage(SaveImageResult.Success(uri)),
            Event.ShareImage(uri, page),
            Event.CopyImage(uri),
            Event.SetCoverResult(SetAsCoverResult.Success),
        )
        runBlocking { events.forEach { activity.viewModel.eventChannel.send(it) } }
        harness.settle()
        activity.requestedOrientation shouldBe ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity.viewModel.updateState { it.copy(viewerChapters = null) }
        runBlocking { activity.viewModel.eventChannel.send(Event.ReloadViewerChapters) }
    }

    @Test
    fun incognitoOffFinishes() {
        val activity = harness.launch().get()
        harness.vm.basePreferences.incognitoMode.set(true)
        harness.settle()
        activity.isFinishing shouldBe false
        harness.vm.basePreferences.incognitoMode.set(false)
        harness.settle()
        activity.isFinishing shouldBe true
    }
}
