package eu.kanade.tachiyomi.ui.manga

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreen
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class MangaScreenActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MangaHarness()
    private val host = MangaActionsHost(compose)
    private val http = mockk<HttpSource>(relaxed = true) {
        every { id } returns 7L
        every { getMangaUrl(any()) } returns "https://example.org/m/1"
    }

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun plainSourceHidesWebActions() {
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        host.show(harness.loaded())
        host.actions.header.onWebViewClicked.shouldBeNull()
        host.actions.header.onWebViewLongClicked.shouldBeNull()
        host.actions.header.onEditFetchIntervalClicked.shouldBeNull()
        host.actions.toolbar.onShareClicked.shouldBeNull()
        host.actions.toolbar.onMigrateClicked.shouldBeNull()
        host.actions.chapters.onDownloadChapter.shouldNotBeNull()
    }

    @Test
    fun localSourceHasNoDownloads() {
        every { harness.sourceManager.getOrStub(any()) } returns mockk<LocalSource>(relaxed = true) {
            every { id } returns LocalSource.ID
        }
        harness.mangaFlow.value = manga(source = LocalSource.ID) to listOf(chapter(1L))
        host.show(harness.loaded())
        host.actions.chapters.onDownloadChapter.shouldBeNull()
        host.actions.toolbar.onDownloadActionClicked.shouldBeNull()
    }

    @Test
    fun webActionsOpenAndCopy() {
        every { harness.sourceManager.getOrStub(any()) } returns http
        host.show(harness.loaded())
        host.pushedBy { host.actions.header.onWebViewClicked!!() }.shouldBeInstanceOf<WebViewScreen>()
        host.actions.header.onWebViewLongClicked!!()
        host.actions.toolbar.onShareClicked!!()
        shadowOf(host.context.baseContext as android.app.Activity).nextStartedActivity.shouldNotBeNull()
    }

    @Test
    fun trackingNeedsATracker() {
        val model = harness.loaded()
        host.show(model)
        host.pushedBy { host.actions.header.onTrackingClicked() }.shouldBeInstanceOf<SettingsScreen>()
    }

    @Test
    fun infoActionsNavigate() {
        host.show(harness.loaded())
        host.pushedBy { host.actions.info.onEditNotesClicked() }.shouldBeInstanceOf<MangaNotesScreen>()
        host.pushedBy { host.actions.info.onSearch("q", true) }.shouldBeInstanceOf<GlobalSearchScreen>()
        host.actions.info.onTagSearch("Action")
        host.actions.info.onContinueReading()
        host.actions.info.onCoverClicked()
        host.actions.chapters.onChapterClicked(chapter(1L))
        host.navigator.size shouldBe 3
    }

    @Test
    fun headerActionsReachTheModel() {
        val model = harness.loaded()
        host.show(model)
        host.actions.header.onEditFetchIntervalClicked!!()
        model.awaitSuccess().dialog.shouldBeInstanceOf<MangaScreenModel.Dialog.SetFetchInterval>()
        host.actions.header.onEditCategoryClicked!!()
        host.actions.header.onAddToLibraryClicked()
        host.actions.toolbar.onFilterButtonClicked()
        host.actions.toolbar.onRefresh()
        host.actions.toolbar.onDownloadActionClicked!!(DownloadAction.UNREAD_CHAPTERS)
        host.actions.chapters.onDownloadChapter!!(emptyList(), ChapterDownloadAction.START)
        host.actions.chapters.onChapterSelected(item(chapter(1L)), true, false)
        host.actions.chapters.onChapterSwipe(item(chapter(1L)), LibraryPreferences.ChapterSwipeAction.ToggleBookmark)
        host.pushedBy { host.actions.toolbar.onMigrateClicked!!() }.shouldBeInstanceOf<Any>()
        host.pushedBy { host.actions.toolbar.navigateUp() }.shouldBeInstanceOf<BlankScreen>()
    }
}
