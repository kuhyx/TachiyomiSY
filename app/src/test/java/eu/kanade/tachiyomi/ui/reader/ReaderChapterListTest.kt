package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.util.chapter.filterDownloaded
import exh.source.MERGED_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class ReaderChapterListTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        harness.readerPreferences.skipFiltered.set(false)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        harness.stop()
    }

    private fun ids(vm: ReaderViewModel) = vm.buildChapterList().map { it.chapter.id }

    private fun withFlags(flags: Long, chapterId: Long = 1L): ReaderViewModel =
        harness.loadedViewModel(chapterId).also { vm ->
            vm.updateState { it.copy(manga = harness.manga.copy(chapterFlags = flags)) }
            harness.readerPreferences.skipFiltered.set(true)
        }

    private fun downloaded(name: String) {
        every {
            harness.downloadManager.isChapterDownloaded(
                chapterName = name,
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = any(),
            )
        } returns true
    }

    @Test
    fun plainListIsSorted() {
        harness.chapters(domainChapter(2L, sourceOrder = 0L), domainChapter(1L, sourceOrder = 1L))
        ids(harness.loadedViewModel()) shouldBe listOf(1L, 2L)
    }

    @Test
    fun missingSelectionFails() {
        harness.chapters(domainChapter(2L))
        shouldThrow<IllegalStateException> { harness.loadedViewModel(chapterId = 5L).buildChapterList() }
            .message shouldBe "Requested chapter of id 5 not found in chapter list"
    }

    @Test
    fun skipReadKeepsSelection() {
        harness.chapters(domainChapter(1L, read = true), domainChapter(2L, read = true), domainChapter(3L))
        harness.readerPreferences.skipRead.set(true)
        ids(harness.loadedViewModel(chapterId = 1L)) shouldBe listOf(1L, 3L)
        ids(harness.loadedViewModel(chapterId = 3L)) shouldBe listOf(3L)
    }

    @Test
    fun readFilters() {
        harness.chapters(domainChapter(1L, read = true), domainChapter(2L))
        ids(withFlags(Manga.CHAPTER_SHOW_READ)) shouldBe listOf(1L)
        ids(withFlags(Manga.CHAPTER_SHOW_UNREAD, chapterId = 2L)) shouldBe listOf(2L)
        ids(withFlags(0L)) shouldBe listOf(1L, 2L)
    }

    @Test
    fun bookmarkFilters() {
        harness.chapters(domainChapter(1L, bookmark = true), domainChapter(2L))
        ids(withFlags(Manga.CHAPTER_SHOW_BOOKMARKED)) shouldBe listOf(1L)
        ids(withFlags(Manga.CHAPTER_SHOW_NOT_BOOKMARKED, chapterId = 2L)) shouldBe listOf(2L)
    }

    @Test
    fun downloadFilters() {
        harness.chapters(domainChapter(1L), domainChapter(2L))
        downloaded("Chapter 1")
        ids(withFlags(Manga.CHAPTER_SHOW_DOWNLOADED)) shouldBe listOf(1L)
        ids(withFlags(Manga.CHAPTER_SHOW_NOT_DOWNLOADED, chapterId = 2L)) shouldBe listOf(2L)
    }

    @Test
    fun mergedChaptersUseTheirManga() {
        val merged = harness.manga.copy(source = MERGED_SOURCE_ID, chapterFlags = Manga.CHAPTER_SHOW_DOWNLOADED)
        val child = harness.manga.copy(id = 20L, ogTitle = "Child")
        val chapters = listOf(domainChapter(1L).copy(mangaId = 20L), domainChapter(2L).copy(mangaId = 30L))
        coEvery { harness.getMergedChaptersByMangaId.await(10L, applyScanlatorFilter = true) } returns chapters
        coEvery { harness.getMergedMangaById.await(10L) } returns listOf(child)
        every {
            harness.downloadManager.isChapterDownloaded(
                chapterName = any(),
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = "Child",
                sourceId = any(),
                skipCache = any(),
            )
        } returns true
        harness.readerPreferences.skipFiltered.set(true)
        val vm = harness.viewModel()
        vm.updateState { it.copy(manga = merged) }
        vm.chapterId = 2L
        ids(vm) shouldBe listOf(1L, 2L)
    }

    @Test
    fun dupesAndDownloadedOnly() {
        harness.chapters(domainChapter(1L, number = 1.0), domainChapter(2L, number = 1.0), domainChapter(3L))
        harness.readerPreferences.skipDupe.set(true)
        ids(harness.loadedViewModel(chapterId = 2L)) shouldBe listOf(2L, 3L)
        mockkStatic("eu.kanade.tachiyomi.util.chapter.ChapterFilterDownloadedKt")
        every { any<List<Chapter>>().filterDownloaded(any(), any()) } answers { firstArg<List<Chapter>>().take(1) }
        harness.basePreferences.downloadedOnly.set(true)
        ids(harness.loadedViewModel(chapterId = 2L)) shouldBe listOf(2L)
    }
}
