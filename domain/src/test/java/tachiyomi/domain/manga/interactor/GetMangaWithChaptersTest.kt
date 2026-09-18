package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.repository.MangaRepository

internal class GetMangaWithChaptersTest {

    private val manga = MangaFixtures.manga(id = 4L)
    private val chapters = listOf(
        Chapter.create().copy(id = 1L, mangaId = 4L),
        Chapter.create().copy(id = 2L, mangaId = 4L),
    )
    private val mangaRepository = mockk<MangaRepository>()
    private val chapterRepository = mockk<ChapterRepository>()
    private val interactor = GetMangaWithChapters(mangaRepository, chapterRepository)

    @Test
    fun subscribeCombinesBothFlows() = runTest {
        coEvery { mangaRepository.getMangaByIdAsFlow(4L) } returns flowOf(manga)
        coEvery { chapterRepository.getChapterByMangaIdAsFlow(4L, false) } returns flowOf(chapters)

        val (first, second) = interactor.subscribe(4L).first()

        first shouldBe manga
        second shouldContainExactly chapters
    }

    @Test
    fun subscribePassesFilter() = runTest {
        coEvery { mangaRepository.getMangaByIdAsFlow(4L) } returns flowOf(manga)
        coEvery { chapterRepository.getChapterByMangaIdAsFlow(4L, true) } returns flowOf(emptyList())

        val (first, second) = interactor.subscribe(4L, applyScanlatorFilter = true).first()

        first shouldBe manga
        second shouldBe emptyList()
    }

    @Test
    fun awaitMangaDelegates() = runTest {
        coEvery { mangaRepository.getMangaById(4L) } returns manga

        interactor.awaitManga(4L) shouldBe manga
    }

    @Test
    fun awaitChaptersDefaultsOff() = runTest {
        coEvery { chapterRepository.getChapterByMangaId(4L, false) } returns chapters

        interactor.awaitChapters(4L) shouldContainExactly chapters
    }

    @Test
    fun awaitChaptersPassesFilter() = runTest {
        coEvery { chapterRepository.getChapterByMangaId(4L, true) } returns emptyList()

        interactor.awaitChapters(4L, applyScanlatorFilter = true) shouldBe emptyList()
        coVerify(exactly = 1) { chapterRepository.getChapterByMangaId(4L, true) }
    }
}
