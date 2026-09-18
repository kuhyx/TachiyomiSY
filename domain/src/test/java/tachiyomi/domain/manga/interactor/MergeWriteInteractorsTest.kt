package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeRepository

internal class MergeWriteInteractorsTest {

    private val reference = MergedMangaReference(
        id = 1L,
        isInfoManga = false,
        getChapterUpdates = true,
        chapterSortMode = MergedMangaReference.CHAPTER_SORT_NO_DEDUPE,
        chapterPriority = 1,
        downloadChapters = true,
        mergeId = null,
        mergeUrl = "/merge",
        mangaId = null,
        mangaUrl = "/m",
        mangaSourceId = 9L,
    )
    private val settings = MergeMangaSettingsUpdate(
        id = 1L,
        isInfoManga = true,
        getChapterUpdates = null,
        chapterPriority = null,
        downloadChapters = null,
        chapterSortMode = null,
    )
    private val repository = mockk<MangaMergeRepository>()

    @Test
    fun deleteByMergeIdDelegates() = runTest {
        coEvery { repository.deleteByMergeId(2L) } returns Unit

        DeleteByMergeId(repository).await(2L)

        coVerify(exactly = 1) { repository.deleteByMergeId(2L) }
    }

    @Test
    fun deleteMergeByIdDelegates() = runTest {
        coEvery { repository.deleteById(1L) } returns Unit

        DeleteMergeById(repository).await(1L)

        coVerify(exactly = 1) { repository.deleteById(1L) }
    }

    @Test
    fun insertReferenceReturnsId() = runTest {
        coEvery { repository.insert(reference) } returns 77L

        InsertMergedReference(repository).await(reference) shouldBe 77L
    }

    @Test
    fun insertReferenceMayBeNull() = runTest {
        coEvery { repository.insert(reference) } returns null

        InsertMergedReference(repository).await(reference) shouldBe null
    }

    @Test
    fun insertAllReferencesDelegates() = runTest {
        val batch = listOf(reference, reference.copy(id = 2L))
        coEvery { repository.insertAll(batch) } returns Unit

        InsertMergedReference(repository).awaitAll(batch)

        coVerify(exactly = 1) { repository.insertAll(batch) }
    }

    @Test
    fun updateSettingsDelegates() = runTest {
        coEvery { repository.updateSettings(settings) } returns true

        UpdateMergedSettings(repository).await(settings) shouldBe true
    }

    @Test
    fun updateAllSettingsDelegates() = runTest {
        val batch = listOf(settings, settings.copy(id = 2L))
        coEvery { repository.updateAllSettings(batch) } returns false

        UpdateMergedSettings(repository).awaitAll(batch) shouldBe false
    }
}
