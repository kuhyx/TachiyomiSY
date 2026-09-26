package eu.kanade.tachiyomi.ui.library

import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import exh.md.utils.FollowStatus
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo

@RunWith(RobolectricTestRunner::class)
internal class LibraryBulkActionsTest {
    private val harness = LibraryHarness()
    private val savedNHentai = nHentaiSourceIds
    private val savedMangaDex = mangaDexSourceIds

    @Before
    fun setUp() {
        nHentaiSourceIds = listOf(NHENTAI)
        mangaDexSourceIds = listOf(MANGADEX)
        mainUnconfined()
        startKoin { modules(harness.koinModules() + module { single { GetCustomMangaInfo(EditedInfo) } }) }
    }

    @After
    fun tearDown() {
        nHentaiSourceIds = savedNHentai
        mangaDexSourceIds = savedMangaDex
        mainReset()
        stopKoin()
    }

    private fun LibraryScreenModel.awaitCleared() = state.await { it.selection.isEmpty() }

    @Test
    fun downloadActions() {
        coEvery { harness.getBookmarked.await(1L) } returns emptyList()
        coEvery { harness.getNextChapters.await(1L) } returns emptyList()
        val model = harness.selectedModel(manga(1))
        model.performDownloadAction(DownloadAction.BOOKMARKED_CHAPTERS)
        model.awaitCleared()
        coVerify(timeout = WAIT) { harness.getBookmarked.await(1L) }
        model.updateState { it.copy(selection = setOf(1L)) }
        model.performDownloadAction(DownloadAction.NEXT_5_CHAPTERS)
        coVerify(timeout = WAIT) { harness.getNextChapters.await(1L) }
    }

    @Test
    fun cleanTitlesOfGallerySources() {
        val edited = manga(18).copy(source = EH_SOURCE_ID, favorite = true)
        val model = harness.selectedModel(
            edited,
            manga(1, "Plain").copy(source = EH_SOURCE_ID),
            manga(2, "(x) Name {y} | Real2").copy(source = NHENTAI),
            manga(3, "[x] Other").copy(source = 5),
            manga(4, "[only]").copy(source = EH_SOURCE_ID),
        )
        model.cleanTitles()
        model.awaitCleared()
        verify {
            harness.setCustomMangaInfo.set(
                CustomMangaInfo(
                    id = 18,
                    title = "Real",
                    author = "a",
                    artist = "a",
                    thumbnailUrl = "a",
                    description = "a",
                    genre = listOf("a"),
                    status = 1,
                ),
            )
        }
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 2, title = "Real2")) }
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 4, title = null)) }
        verify(exactly = 3) { harness.setCustomMangaInfo.set(any()) }
    }

    @Test
    fun resetInfoClearsEveryField() {
        val model = harness.selectedModel(manga(1))
        model.resetInfo()
        model.awaitCleared()
        verify { harness.setCustomMangaInfo.set(CustomMangaInfo(id = 1, title = null)) }
    }

    @Test
    fun markReadSelection() {
        // Suspends before answering, so the call resumes rather than returning at once.
        coEvery { harness.setReadStatus.await(manga = any(), read = any()) } coAnswers {
            delay(1)
            SetReadStatus.Result.Success
        }
        val model = harness.selectedModel(manga(1))
        model.markReadSelection(read = true)
        model.awaitCleared()
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = any(), read = true) }
        model.updateState { it.copy(selection = setOf(1L)) }
        model.markReadSelection(read = false)
        coVerify(timeout = WAIT) { harness.setReadStatus.await(manga = any(), read = false) }
    }

    @Test
    fun mangadexWithoutASource() {
        every { harness.sourceManager.getVisibleOnlineSources() } returns emptyList()
        harness.selectedModel(manga(1)).apply { syncMangaToDex() }.awaitCleared()
    }

    @Test
    fun mangadexFollowsTheSelection() {
        val dex = mockk<MangaDex>(relaxed = true) {
            every { id } returns MANGADEX
            every { lang } returns "en"
        }
        every { harness.sourceManager.getVisibleOnlineSources() } returns listOf(dex)
        harness.sourcePreferences.enabledLanguages.set(setOf("en"))
        val model = harness.selectedModel(manga(1).copy(source = MANGADEX, url = "/title/abc/"), manga(2))
        model.syncMangaToDex()
        model.awaitCleared()
        coVerify(exactly = 1) { dex.updateFollowStatus("abc", FollowStatus.READING) }
    }

    private companion object {
        const val NHENTAI = 6_907L
        const val MANGADEX = 2_499L
        const val WAIT = 5_000L
    }
}
