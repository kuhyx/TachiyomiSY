package tachiyomi.domain.manga.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeRepository

internal class MergeReadInteractorsTest {

    private val manga = MangaFixtures.manga(id = 4L)
    private val reference = MergedMangaReference(
        id = 1L,
        isInfoManga = true,
        getChapterUpdates = true,
        chapterSortMode = MergedMangaReference.CHAPTER_SORT_NONE,
        chapterPriority = 0,
        downloadChapters = false,
        mergeId = 2L,
        mergeUrl = "/merge",
        mangaId = 4L,
        mangaUrl = "/m",
        mangaSourceId = 9L,
    )
    private val repository = mockk<MangaMergeRepository>()

    @Test
    fun mergedMangaAwaitDelegates() = runTest {
        coEvery { repository.getMergedManga() } returns listOf(manga)

        GetMergedManga(repository).await() shouldContainExactly listOf(manga)
    }

    @Test
    fun mergedMangaAwaitSwallows() = runTest {
        coEvery { repository.getMergedManga() } throws IllegalStateException("store failed")

        GetMergedManga(repository).await() shouldBe emptyList()
    }

    @Test
    fun mergedMangaSubscribeWorks() = runTest {
        coEvery { repository.subscribeMergedManga() } returns flowOf(listOf(manga))

        GetMergedManga(repository).subscribe().first() shouldContainExactly listOf(manga)
    }

    @Test
    fun mergedByIdAwaitDelegates() = runTest {
        coEvery { repository.getMergedMangaById(2L) } returns listOf(manga)

        GetMergedMangaById(repository).await(2L) shouldContainExactly listOf(manga)
    }

    @Test
    fun mergedByIdAwaitSwallows() = runTest {
        coEvery { repository.getMergedMangaById(2L) } throws IllegalStateException("store failed")

        GetMergedMangaById(repository).await(2L) shouldBe emptyList()
    }

    @Test
    fun mergedByIdSubscribeDelegates() = runTest {
        coEvery { repository.subscribeMergedMangaById(2L) } returns flowOf(listOf(manga))

        GetMergedMangaById(repository).subscribe(2L).first() shouldContainExactly listOf(manga)
    }

    @Test
    fun referencesAwaitDelegates() = runTest {
        coEvery { repository.getReferencesById(2L) } returns listOf(reference)

        GetMergedReferencesById(repository).await(2L) shouldContainExactly listOf(reference)
    }

    @Test
    fun referencesAwaitSwallows() = runTest {
        coEvery { repository.getReferencesById(2L) } throws IllegalStateException("store failed")

        GetMergedReferencesById(repository).await(2L) shouldBe emptyList()
    }

    @Test
    fun referencesSubscribeDelegates() = runTest {
        coEvery { repository.subscribeReferencesById(2L) } returns flowOf(listOf(reference))

        GetMergedReferencesById(repository).subscribe(2L).first() shouldContainExactly listOf(reference)
    }

    @Test
    fun forDownloadingDelegates() = runTest {
        coEvery { repository.getMergeMangaForDownloading(2L) } returns listOf(manga)

        GetMergedMangaForDownloading(repository).await(2L) shouldContainExactly listOf(manga)
    }
}
