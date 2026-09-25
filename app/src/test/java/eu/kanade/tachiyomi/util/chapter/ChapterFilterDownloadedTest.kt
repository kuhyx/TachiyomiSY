package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.isChapterDownloaded
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

internal class ChapterFilterDownloadedTest {

    private val cache = mockk<DownloadCache>()
    private val manga = Manga.create().copy(id = 1, source = 7, ogTitle = "T")
    private val merged = Manga.create().copy(id = 2, source = 9, ogTitle = "M")
    private val chapters = listOf(
        Chapter.create().copy(id = 1, mangaId = 1, url = "/a"),
        Chapter.create().copy(id = 2, mangaId = 2, url = "/b"),
        Chapter.create().copy(id = 3, mangaId = 1, url = "/c"),
    )

    @BeforeEach
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadCacheQueriesKt")
        startKoin { modules(module { single { cache } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun localMangaKeepsEverything() {
        chapters.filterDownloaded(manga.copy(source = LocalSource.ID), null) shouldBe chapters
    }

    @Test
    fun keepsOnlyDownloadedChapters() {
        every { cache.isChapterDownloaded(any(), any(), "/a", "T", 7, false) } returns true
        every { cache.isChapterDownloaded(any(), any(), "/b", "M", 9, false) } returns true
        every { cache.isChapterDownloaded(any(), any(), "/c", "T", 7, false) } returns false
        chapters.filterDownloaded(manga, mapOf(2L to merged)).map { it.id } shouldBe listOf(1L, 2L)
        every { cache.isChapterDownloaded(any(), any(), "/b", "T", 7, false) } returns false
        chapters.filterDownloaded(manga, null).map { it.id } shouldBe listOf(1L)
        chapters.filterDownloaded(manga, emptyMap()).map { it.id } shouldBe listOf(1L)
    }
}
