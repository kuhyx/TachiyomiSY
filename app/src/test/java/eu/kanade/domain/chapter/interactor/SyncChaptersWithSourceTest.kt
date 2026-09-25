package eu.kanade.domain.chapter.interactor

import eu.kanade.tachiyomi.data.download.isChapterDirNameChanged
import eu.kanade.tachiyomi.data.download.renameChapter
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.source.local.LocalSource

internal class SyncChaptersWithSourceTest {

    private val harness = SyncChaptersHarness()
    private val interactor = harness.interactor
    private val manga = libraryManga()
    private val source = mockk<Source> { every { id } returns 7L }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun refusesAnEmptyRemoteList() = runTest {
        shouldThrow<NoChaptersException> { interactor.await(emptyList(), manga, source) }
    }

    @Test
    fun localSourceMayBeEmpty() = runTest {
        val local = mockk<Source> { every { id } returns LocalSource.ID }
        coEvery { harness.getChaptersByMangaId.await(1) } returns emptyList()
        interactor.await(emptyList(), manga.copy(fetchInterval = 3, nextUpdate = 1), local) shouldBe emptyList()
        coVerify(exactly = 0) { harness.updateManga.awaitUpdateFetchInterval(any(), any(), any()) }
    }

    @Test
    fun refreshesIntervalIfUnchanged() = runTest {
        val db = listOf(dbChapter("/a"))
        coEvery { harness.getChaptersByMangaId.await(1) } returns db
        coEvery { harness.shouldUpdateDbChapter.await(any(), any()) } returns false
        coEvery { harness.updateManga.awaitUpdateFetchInterval(any(), any(), any()) } returns true
        val chapters = listOf(sChapter("/a"))
        interactor.await(chapters, manga, source, manualFetch = true) shouldBe emptyList()
        interactor.await(chapters, manga.copy(fetchInterval = 0), source) shouldBe emptyList()
        interactor.await(chapters, manga.copy(fetchInterval = 3, nextUpdate = 5), source, fetchWindow = 10L to 20L)
        interactor.await(chapters, manga.copy(fetchInterval = 3, nextUpdate = 15), source, fetchWindow = 10L to 20L)
        coVerify(exactly = 3) { harness.updateManga.awaitUpdateFetchInterval(any(), any(), any()) }
    }

    @Test
    fun addsNewWithBorrowedDates() = runTest {
        harness.stubWrites()
        coEvery { harness.getChaptersByMangaId.await(1) } returns emptyList()
        val added = slot<List<Chapter>>()
        coEvery { harness.chapterRepository.addAll(capture(added)) } answers { firstArg() }
        val chapters = listOf(
            sChapter("/c", name = "Chapter 3", dateUpload = 0),
            sChapter("/b", name = "Chapter 2", dateUpload = 100),
            sChapter("/a", name = "Chapter 1", dateUpload = 0),
            sChapter("/a", name = "duplicate url"),
        )
        val result = interactor.await(chapters, manga, source)
        result.size shouldBe 3
        added.captured.map { it.url } shouldBe listOf("/c", "/b", "/a")
        added.captured.map { it.chapterNumber } shouldBe listOf(3.0, 2.0, 1.0)
        added.captured.map { it.sourceOrder } shouldBe listOf(0L, 1L, 2L)
        added.captured[1].dateUpload shouldBe 100L
        added.captured[2].dateUpload shouldBe 100L
        (added.captured[0].dateUpload > 100L) shouldBe true
        added.captured.map { it.dateFetch }.zipWithNext().all { (a, b) -> a > b } shouldBe true
        coVerify(exactly = 0) { harness.chapterRepository.removeChaptersWithIds(any()) }
        coVerify(exactly = 0) { harness.updateChapter.awaitAll(any()) }
    }

    @Test
    fun filtersExcludedAndDuplicates() = runTest {
        harness.stubWrites()
        harness.libraryPreferences.markDuplicateReadChapterAsRead.set(setOf("new"))
        coEvery { harness.getExcludedScanlators.await(1) } returns setOf("bad")
        val db = listOf(dbChapter("/old", chapterNumber = 1.0, read = true))
        coEvery { harness.getChaptersByMangaId.await(1) } returns db
        coEvery { harness.shouldUpdateDbChapter.await(any(), any()) } returns false
        val chapters = listOf(
            sChapter("/old", name = "Chapter 1"),
            sChapter("/dup", name = "Chapter 1"),
            sChapter("/x", name = "Chapter 2").apply { scanlator = "bad" },
            sChapter("/y", name = "Chapter 3"),
        )
        interactor.await(chapters, manga, source).map { it.url } shouldBe listOf("/y")
    }

    @Test
    fun updatesChangedAndRenames() = runTest {
        harness.stubWrites()
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadProviderNamesKt")
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerRenamesKt")
        val db = listOf(dbChapter("/a"), dbChapter("/b"), dbChapter("/c"), dbChapter("/gone"))
        coEvery { harness.getChaptersByMangaId.await(1) } returns db
        coEvery { harness.shouldUpdateDbChapter.await(any(), any()) } returns true
        every { harness.downloadProvider.isChapterDirNameChanged(db[0], any()) } returns false
        every { harness.downloadProvider.isChapterDirNameChanged(db[1], any()) } returns true
        every { harness.downloadProvider.isChapterDirNameChanged(db[2], any()) } returns true
        every {
            harness.downloadManager.isChapterDownloaded(db[1].name, db[1].scanlator, db[1].url, "Title", 7L)
        } returns false
        every {
            harness.downloadManager.isChapterDownloaded(db[2].name, db[2].scanlator, db[2].url, "Title", 7L)
        } returns true
        coEvery { harness.downloadManager.renameChapter(source, manga, db[2], any()) } returns Unit
        val updates = slot<List<ChapterUpdate>>()
        coEvery { harness.updateChapter.awaitAll(capture(updates)) } returns Unit
        val chapters = listOf(
            sChapter("/a", name = "Chapter 1 renamed"),
            sChapter("/b", name = "Chapter 2 renamed", dateUpload = 50),
            sChapter("/c", name = "Chapter 3 renamed"),
        )
        interactor.await(chapters, manga, source) shouldBe emptyList()
        coVerify(exactly = 1) { harness.downloadManager.renameChapter(source, manga, db[2], any()) }
        coVerify(exactly = 1) { harness.chapterRepository.removeChaptersWithIds(listOf(db[3].id)) }
        updates.captured.map { it.name } shouldBe listOf("Chapter 1 renamed", "Chapter 2 renamed", "Chapter 3 renamed")
        updates.captured.map { it.dateUpload } shouldBe listOf(-1L, 50L, -1L)
        updates.captured.map { it.chapterNumber } shouldBe listOf(1.0, 2.0, 3.0)
    }

    @Test
    fun updatesWithoutAdditions() = runTest {
        harness.stubWrites()
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadProviderNamesKt")
        val db = listOf(dbChapter("/a"))
        coEvery { harness.getChaptersByMangaId.await(1) } returns db
        coEvery { harness.shouldUpdateDbChapter.await(any(), any()) } returns true
        every { harness.downloadProvider.isChapterDirNameChanged(any(), any()) } returns false
        interactor.await(listOf(sChapter("/a", name = "Chapter 1 renamed")), manga, source) shouldBe emptyList()
        coVerify(exactly = 1) { harness.updateChapter.awaitAll(any()) }
        coVerify(exactly = 0) { harness.chapterRepository.addAll(any()) }
    }

    @Test
    fun letsHttpSourcesPrepareChapters() = runTest {
        harness.stubWrites()
        val http = mockk<HttpSource>(relaxed = true) { every { id } returns 7L }
        coEvery { harness.getChaptersByMangaId.await(1) } returns emptyList()
        val result = interactor.await(listOf(sChapter("/a", name = "Ch.4")), manga, http)
        result.single().chapterNumber shouldBe 4.0
    }
}
