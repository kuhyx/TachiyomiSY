package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.chapterOf
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.MergedMangaReference
import java.io.IOException

internal class GetMergedChaptersByMangaIdTest {

    private val a1 = chapterOf(1L, 10L, 1.0)
    private val a2 = chapterOf(2L, 10L, 2.0)
    private val b1 = chapterOf(3L, 11L, 1.0)
    private val chapters = listOf(a1, a2, b1)
    private val references = listOf(mergeReference(MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS))
    private val repository = mockk<ChapterRepository>()
    private val getReferences = mockk<GetMergedReferencesById>()
    private val getMerged = GetMergedChaptersByMangaId(repository, getReferences)

    @Test
    fun awaitDedupesByDefault() = runTest {
        coEvery { getReferences.await(99L) } returns references
        coEvery { repository.getMergedChapterByMangaId(99L, false) } returns chapters

        getMerged.await(99L) shouldBe listOf(a1, a2)
    }

    @Test
    fun awaitPassesFlagsThrough() = runTest {
        coEvery { getReferences.await(99L) } returns references
        coEvery { repository.getMergedChapterByMangaId(99L, true) } returns chapters

        getMerged.await(99L, dedupe = false, applyScanlatorFilter = true) shouldBe chapters
    }

    @Test
    fun awaitStoreFailureIsEmpty() = runTest {
        coEvery { getReferences.await(99L) } returns references
        coEvery { repository.getMergedChapterByMangaId(99L, false) } throws IOException("store down")

        getMerged.await(99L) shouldBe emptyList()
    }

    @Test
    fun subscribeDedupesByDefault() = runTest {
        coEvery { getReferences.subscribe(99L) } returns flowOf(references)
        coEvery { repository.getMergedChapterByMangaIdFlow(99L, false) } returns flowOf(chapters)

        getMerged.subscribe(99L).first() shouldBe listOf(a1, a2)
    }

    @Test
    fun subscribePassesFlagsThrough() = runTest {
        coEvery { getReferences.subscribe(99L) } returns flowOf(references)
        coEvery { repository.getMergedChapterByMangaIdFlow(99L, true) } returns flowOf(chapters)

        getMerged.subscribe(99L, dedupe = false, applyScanlatorFilter = true).first() shouldBe chapters
    }

    @Test
    fun subscribeStoreFailureIsEmpty() = runTest {
        coEvery { repository.getMergedChapterByMangaIdFlow(99L, false) } throws IOException("store down")

        getMerged.subscribe(99L).first() shouldBe emptyList()
    }
}
