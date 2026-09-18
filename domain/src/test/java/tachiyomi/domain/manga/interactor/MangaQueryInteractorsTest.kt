package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaRepository

internal class MangaQueryInteractorsTest {

    private val manga = MangaFixtures.manga(id = 4L).copy(ogTitle = "Mixed Case", url = "/m", source = 9L)
    private val repository = mockk<MangaRepository>()

    @Test
    fun getAllMangaDelegates() = runTest {
        coEvery { repository.getAll() } returns listOf(manga)

        GetAllManga(repository).await() shouldContainExactly listOf(manga)
    }

    @Test
    fun duplicatesUseLowercaseTitle() = runTest {
        val duplicate = MangaWithChapterCount(manga = manga.copy(id = 5L), chapterCount = 2L)
        coEvery { repository.getDuplicateLibraryManga(4L, "mixed case") } returns listOf(duplicate)

        GetDuplicateLibraryManga(repository)(manga) shouldContainExactly listOf(duplicate)
    }

    @Test
    fun favoritesAwaitDelegates() = runTest {
        coEvery { repository.getFavorites() } returns listOf(manga)

        GetFavorites(repository).await() shouldContainExactly listOf(manga)
    }

    @Test
    fun favoritesSubscribeDelegates() = runTest {
        every { repository.getFavoritesBySourceId(9L) } returns flowOf(listOf(manga))

        GetFavorites(repository).subscribe(9L).first() shouldContainExactly listOf(manga)
    }

    @Test
    fun mangaBySourceDelegates() = runTest {
        coEvery { repository.getMangaBySourceId(9L) } returns listOf(manga)

        GetMangaBySource(repository).await(9L) shouldContainExactly listOf(manga)
    }

    @Test
    fun mangaByUrlAndSourceDelegates() = runTest {
        coEvery { repository.getMangaByUrlAndSourceId("/m", 9L) } returns manga
        coEvery { repository.getMangaByUrlAndSourceId("/x", 9L) } returns null

        GetMangaByUrlAndSourceId(repository).await("/m", 9L) shouldBe manga
        GetMangaByUrlAndSourceId(repository).await("/x", 9L) shouldBe null
    }

    @Test
    fun readNotInLibraryDelegates() = runTest {
        val libraryManga = LibraryManga(
            manga = manga,
            categories = emptyList(),
            totalChapters = 1L,
            readCount = 1L,
            bookmarkCount = 0L,
            latestUpload = 0L,
            chapterFetchedAt = 0L,
            lastRead = 0L,
        )
        coEvery { repository.getReadMangaNotInLibraryView() } returns listOf(libraryManga)

        GetReadMangaNotInLibraryView(repository).await() shouldContainExactly listOf(libraryManga)
    }
}
