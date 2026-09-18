package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.repository.MangaRepository

internal class GetLibraryMangaTest {

    private val libraryManga = LibraryManga(
        manga = MangaFixtures.manga(id = 4L),
        categories = listOf(1L),
        totalChapters = 3L,
        readCount = 1L,
        bookmarkCount = 0L,
        latestUpload = 5L,
        chapterFetchedAt = 6L,
        lastRead = 7L,
    )
    private val repository = mockk<MangaRepository>()
    private val getLibraryManga = GetLibraryManga(repository)

    @Test
    fun awaitDelegates() = runTest {
        coEvery { repository.getLibraryManga() } returns listOf(libraryManga)

        getLibraryManga.await() shouldContainExactly listOf(libraryManga)
    }

    @Test
    fun subscribeEmitsTheLibrary() = runTest {
        every { repository.getLibraryMangaAsFlow() } returns flowOf(listOf(libraryManga))

        getLibraryManga.subscribe().toList() shouldContainExactly listOf(listOf(libraryManga))
    }

    @Test
    fun subscribeRetriesAfterNpe() = runTest {
        var attempts = 0
        every { repository.getLibraryMangaAsFlow() } returns flow {
            attempts += 1
            if (attempts == 1) throw NullPointerException("cursor closed")
            emit(listOf(libraryManga))
        }

        getLibraryManga.subscribe().toList() shouldContainExactly listOf(listOf(libraryManga))
        attempts shouldBe 2
    }

    @Test
    fun subscribeSwallowsOtherErrors() = runTest {
        var attempts = 0
        every { repository.getLibraryMangaAsFlow() } returns flow {
            attempts += 1
            error("store failed")
        }

        getLibraryManga.subscribe().toList() shouldBe emptyList()
        attempts shouldBe 1
    }
}
