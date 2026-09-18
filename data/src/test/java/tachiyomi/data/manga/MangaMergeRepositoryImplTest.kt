package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsList
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference

internal class MangaMergeRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = MangaMergeRepositoryImpl(database)

    private suspend fun seed(): Pair<Long, Long> {
        val merge = database.insertManga(url = "/merge", source = MERGED_SOURCE_ID)
        val part = database.insertManga(url = "/a")
        return merge to part
    }

    private fun reference(merge: Long, part: Long?): MergedMangaReference = MergedMangaReference(
        id = -1L, isInfoManga = true, getChapterUpdates = false, chapterSortMode = 3, chapterPriority = 4,
        downloadChapters = true, mergeId = merge, mergeUrl = "/merge", mangaId = part, mangaUrl = "/a",
        mangaSourceId = 1L,
    )

    @Test
    fun insertReturnsId() = runTest {
        val (merge, part) = seed()

        val id = repository.insert(reference(merge = merge, part = part))

        id shouldNotBe null
        val stored = repository.getReferencesById(merge).single()
        stored.id shouldBe id
        stored.isInfoManga shouldBe true
        stored.chapterSortMode shouldBe 3
        stored.chapterPriority shouldBe 4
        stored.mangaId shouldBe part
    }

    @Test
    fun insertAllStoresEach() = runTest {
        val (merge, part) = seed()
        repository.insertAll(emptyList())

        repository.insertAll(listOf(reference(merge = merge, part = part), reference(merge = merge, part = null)))

        repository.getReferencesById(merge).map { it.mangaId } shouldBe listOf(part, null)
    }

    @Test
    fun updateSettingsChangesRow() = runTest {
        val (merge, part) = seed()
        val id = requireNotNull(repository.insert(reference(merge = merge, part = part)))
        val update = MergeMangaSettingsUpdate(
            id = id, isInfoManga = false, getChapterUpdates = true, chapterPriority = 7, downloadChapters = false,
            chapterSortMode = 8,
        )

        repository.updateSettings(update) shouldBe true

        val stored = repository.getReferencesById(merge).single()
        stored.isInfoManga shouldBe false
        stored.getChapterUpdates shouldBe true
        stored.chapterPriority shouldBe 7
        stored.downloadChapters shouldBe false
        stored.chapterSortMode shouldBe 8
    }

    @Test
    fun updateAllSettingsKeepsNulls() = runTest {
        val (merge, part) = seed()
        val id = requireNotNull(repository.insert(reference(merge = merge, part = part)))
        val update = MergeMangaSettingsUpdate(
            id = id, isInfoManga = null, getChapterUpdates = null, chapterPriority = null, downloadChapters = null,
            chapterSortMode = null,
        )

        repository.updateAllSettings(emptyList()) shouldBe true
        repository.updateAllSettings(listOf(update)) shouldBe true

        val stored = repository.getReferencesById(merge).single()
        stored.chapterSortMode shouldBe 3
        stored.chapterPriority shouldBe 4
    }

    @Test
    fun updateAllSettingsFalseOnErr() = runTest {
        val failing = mockk<Database> {
            coEvery { transaction(any(), any()) } throws IllegalStateException("boom")
        }
        val update = MergeMangaSettingsUpdate(
            id = 1L, isInfoManga = null, getChapterUpdates = null, chapterPriority = null, downloadChapters = null,
            chapterSortMode = null,
        )

        MangaMergeRepositoryImpl(failing).updateAllSettings(listOf(update)) shouldBe false
    }

    @Test
    fun deleteById() = runTest {
        val (merge, part) = seed()
        val id = requireNotNull(repository.insert(reference(merge = merge, part = part)))
        repository.insert(reference(merge = merge, part = null))

        repository.deleteById(id)

        repository.getReferencesById(merge).map { it.mangaId } shouldBe listOf(null)
    }

    @Test
    fun deleteByMergeId() = runTest {
        val (merge, part) = seed()
        repository.insert(reference(merge = merge, part = part))
        repository.insert(reference(merge = merge, part = null))

        repository.deleteByMergeId(merge)

        database.mergedQueries.selectAll().awaitAsList() shouldBe emptyList()
    }
}
