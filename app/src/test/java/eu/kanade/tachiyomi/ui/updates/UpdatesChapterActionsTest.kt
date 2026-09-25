package eu.kanade.tachiyomi.ui.updates

import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.model.Manga

internal class UpdatesChapterActionsTest {
    private val harness = UpdatesHarness()
    private val chapter = Chapter.create().copy(id = 1)
    private val manga = Manga.create().copy(id = 1, source = 5)
    private val source = mockk<Source>()

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        coEvery { harness.getChapter.await(1L) } returns chapter
        coEvery { harness.getChapter.await(2L) } returns null
        coEvery { harness.setReadStatus.await(any(), *anyVararg()) } returns SetReadStatus.Result.Success
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        every { harness.downloadManager.deleteChapters(any(), any(), any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun markingReadUsesKnownChapters() {
        harness.model().markUpdatesRead(listOf(item(1), item(2)), read = true)
        coVerify(timeout = WAIT) { harness.setReadStatus.await(true, chapter) }
    }

    @Test
    fun bookmarkingSkipsUnchanged() {
        val bookmarked = item(2).copy(update = update(2, bookmark = true))
        harness.model().bookmarkUpdates(listOf(item(1), bookmarked), bookmark = true)
        coVerify(timeout = WAIT) { harness.updateChapter.awaitAll(listOf(ChapterUpdate(id = 1, bookmark = true))) }
        harness.model().bookmarkUpdates(listOf(bookmarked), bookmark = false)
        coVerify(timeout = WAIT) { harness.updateChapter.awaitAll(listOf(ChapterUpdate(id = 2, bookmark = false))) }
    }

    @Test
    fun deletingNeedsMangaAndSource() {
        coEvery { harness.getManga.await(1L) } returns manga
        coEvery { harness.getManga.await(2L) } returns manga.copy(id = 2, source = 6)
        coEvery { harness.getManga.await(3L) } returns null
        every { harness.sourceManager.get(5L) } returns source
        every { harness.sourceManager.get(6L) } returns null
        harness.model().deleteChapters(listOf(item(1), item(2), item(4, mangaId = 2), item(5, mangaId = 3)))
        verify(timeout = WAIT) { harness.downloadManager.deleteChapters(listOf(chapter), manga, source) }
        coVerify(timeout = WAIT) { harness.getManga.await(3L) }
        verify(exactly = 1) { harness.downloadManager.deleteChapters(any(), any(), any()) }
    }
}

private const val WAIT = 5_000L
