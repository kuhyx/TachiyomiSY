package eu.kanade.domain.chapter.model

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

internal class ChapterFilterTest {

    private val downloadManager = mockk<DownloadManager>()
    private val manga = Manga.create().copy(
        id = 1,
        source = 7,
        ogTitle = "T",
        chapterFlags = Manga.CHAPTER_SORTING_NUMBER,
    )
    private val chapters = listOf(
        chapter(id = 1, number = 1.0, read = true, bookmark = true),
        chapter(id = 2, number = 2.0, read = false, bookmark = false),
        chapter(id = 3, number = 3.0, read = false, bookmark = true, mangaId = 2),
    )

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { BasePreferences(mockk(relaxed = true), InMemoryPreferenceStore()) } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun noFiltersOnlySorts() {
        every { downloadManager.isChapterDownloaded(any(), any(), any(), any(), any()) } returns false
        chapters.applyFilters(manga, downloadManager, emptyMap()).map { it.id } shouldBe listOf(3L, 2L, 1L)
    }

    @Test
    fun filtersUnreadAndBookmarked() {
        val flags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_BOOKMARKED
        val filtered = manga.copy(chapterFlags = flags)
        chapters.applyFilters(filtered, downloadManager, emptyMap()).map { it.id } shouldBe listOf(3L)
    }

    @Test
    fun filtersDownloadedPerSource() {
        val merged = Manga.create().copy(id = 2, source = 9, ogTitle = "M")
        every { downloadManager.isChapterDownloaded(any(), any(), any(), "T", 7) } returns true
        every { downloadManager.isChapterDownloaded(any(), any(), any(), "M", 9) } returns false
        val downloaded = manga.copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SHOW_DOWNLOADED)
        chapters.applyFilters(downloaded, downloadManager, mapOf(2L to merged)).map { it.id } shouldBe listOf(2L, 1L)
        val notDownloaded = manga.copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SHOW_NOT_DOWNLOADED)
        chapters.applyFilters(notDownloaded, downloadManager, mapOf(2L to merged)).map { it.id } shouldBe listOf(3L)
    }

    @Test
    fun filtersReadAndNotBookmarked() {
        val flags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SHOW_READ or Manga.CHAPTER_SHOW_NOT_BOOKMARKED
        chapters.applyFilters(manga.copy(chapterFlags = flags), downloadManager, emptyMap()) shouldBe emptyList()
        val readOnly = manga.copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SHOW_READ)
        chapters.applyFilters(readOnly, downloadManager, emptyMap()).map { it.id } shouldBe listOf(1L)
    }

    @Test
    fun localMangaCountsAsDownloaded() {
        every { downloadManager.isChapterDownloaded(any(), any(), any(), any(), any()) } returns false
        every { downloadManager.isChapterDownloaded("Chapter 2.0", any(), any(), any(), any()) } returns true
        val local = manga.copy(source = LocalSource.ID, chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED)
        chapters.applyFilters(local, downloadManager, emptyMap()).size shouldBe 3
        val notLocal = manga.copy(chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED)
        chapters.applyFilters(notLocal, downloadManager, emptyMap()).map { it.id } shouldBe listOf(2L)
        val localNotDownloaded = manga.copy(source = LocalSource.ID, chapterFlags = Manga.CHAPTER_SHOW_NOT_DOWNLOADED)
        chapters.applyFilters(localNotDownloaded, downloadManager, emptyMap()) shouldBe emptyList()
    }

    @Test
    fun filtersListItemsTheSameWay() {
        val items = chapters.mapIndexed { index, chapter ->
            ChapterList.Item(
                chapter = chapter,
                downloadState = if (index == 1) Download.State.DOWNLOADED else Download.State.NOT_DOWNLOADED,
                downloadProgress = 0,
                sourceName = null,
                showScanlator = false,
            )
        }
        items.applyFilters(manga).map { it.id }.toList() shouldBe listOf(3L, 2L, 1L)
        val unread = manga.copy(chapterFlags = Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_NOT_BOOKMARKED)
        items.applyFilters(unread).map { it.id }.toList() shouldBe listOf(2L)
        val downloaded = manga.copy(chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED)
        items.applyFilters(downloaded).map { it.id }.toList() shouldBe listOf(2L)
        val local = manga.copy(source = LocalSource.ID, chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED)
        items.applyFilters(local).count() shouldBe 3
        val notDownloaded = manga.copy(chapterFlags = Manga.CHAPTER_SHOW_NOT_DOWNLOADED or Manga.CHAPTER_SHOW_READ)
        items.applyFilters(notDownloaded).map { it.id }.toList() shouldBe listOf(1L)
        val localNotDownloaded = manga.copy(source = LocalSource.ID, chapterFlags = Manga.CHAPTER_SHOW_NOT_DOWNLOADED)
        items.applyFilters(localNotDownloaded).count() shouldBe 0
    }

    private fun chapter(id: Long, number: Double, read: Boolean, bookmark: Boolean, mangaId: Long = 1): Chapter =
        Chapter.create().copy(
            id = id,
            mangaId = mangaId,
            url = "/$id",
            name = "Chapter $number",
            chapterNumber = number,
            read = read,
            bookmark = bookmark,
        )
}
