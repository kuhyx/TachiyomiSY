package eu.kanade.tachiyomi.ui.manga

import exh.source.MERGED_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference

@RunWith(RobolectricTestRunner::class)
internal class MangaMergerTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val merger by lazy { MangaMerger(harness.app) }
    private val original = manga().copy(id = 1L, url = "/orig", source = 7L)
    private val incoming = manga().copy(id = 2L, url = "/new", source = 8L)
    private val saved = slot<List<MergedMangaReference>>()

    @Before
    fun setUp() {
        harness.start()
        coEvery { parts.insertMergedReference.awaitAll(capture(saved)) } returns Unit
    }

    @After
    fun tearDown() = harness.stop()

    private fun reference(sourceId: Long, url: String = "/x") = MergedMangaReference(
        id = 3L,
        isInfoManga = false,
        getChapterUpdates = true,
        chapterSortMode = 0,
        chapterPriority = 0,
        downloadChapters = true,
        mergeId = 1L,
        mergeUrl = "/orig",
        mangaId = 4L,
        mangaUrl = url,
        mangaSourceId = sourceId,
    )

    @Test
    fun unknownOriginalFails() {
        coEvery { parts.getManga.await(1L) } returns null
        shouldThrow<IllegalArgumentException> { runBlocking { merger.smartSearchMerge(incoming, 1L) } }
    }

    @Test
    fun addsToAnEmptyMerge() {
        val merged = original.copy(source = MERGED_SOURCE_ID)
        coEvery { parts.getManga.await(1L) } returns merged
        coEvery { harness.getMergedReferences.await(1L) } returns emptyList()
        runBlocking { merger.smartSearchMerge(incoming, 1L) } shouldBe merged
        saved.captured.map { it.mangaSourceId } shouldBe listOf(8L, MERGED_SOURCE_ID)
    }

    @Test
    fun addsBesideTheSelfReference() {
        coEvery { parts.getManga.await(1L) } returns original.copy(source = MERGED_SOURCE_ID)
        coEvery { harness.getMergedReferences.await(1L) } returns listOf(reference(MERGED_SOURCE_ID))
        runBlocking { merger.smartSearchMerge(incoming, 1L) }
        saved.captured.map { it.mangaId } shouldBe listOf(2L)
        coEvery { harness.getMergedReferences.await(1L) } returns listOf(reference(9L))
        runBlocking { merger.smartSearchMerge(incoming, 1L) }
        saved.captured.size shouldBe 2
    }

    @Test
    fun alreadyMergedIsRejected() {
        coEvery { parts.getManga.await(1L) } returns original.copy(source = MERGED_SOURCE_ID)
        coEvery { harness.getMergedReferences.await(1L) } returns listOf(reference(8L, "/new"))
        shouldThrow<IllegalArgumentException> { runBlocking { merger.smartSearchMerge(incoming, 1L) } }
        coEvery { harness.getMergedReferences.await(1L) } returns listOf(reference(8L))
        runBlocking { merger.smartSearchMerge(incoming, 1L) }
    }

    @Test
    fun mergingWithItselfIsRejected() {
        coEvery { parts.getManga.await(1L) } returns original
        shouldThrow<IllegalArgumentException> { runBlocking { merger.smartSearchMerge(original, 1L) } }
    }

    @Test
    fun createsANewMergedEntry() {
        val created = manga().copy(id = 9L, url = "/orig", source = MERGED_SOURCE_ID)
        val stale = created.copy(id = 6L, favorite = false)
        coEvery { parts.getManga.await(1L) } returns original
        coEvery { parts.getManga.await("/orig", MERGED_SOURCE_ID) } returnsMany listOf(stale, null)
        coEvery { parts.networkToLocalManga(any<Manga>()) } returns created
        val category = mockk<Category> { every { id } returns 4L }
        coEvery { parts.getCategories.await(1L) } returns listOf(category)
        runBlocking { merger.smartSearchMerge(incoming, 1L) } shouldBe created
        coVerify { parts.deleteByMergeId.await(6L) }
        coVerify { parts.deleteMangaById.await(6L) }
        coVerify { parts.setMangaCategories.await(9L, listOf(4L)) }
        saved.captured.map { it.mangaId } shouldBe listOf(1L, 2L, 9L)
        saved.captured.map { it.isInfoManga } shouldBe listOf(true, false, false)
    }

    @Test
    fun favouriteDuplicateIsRejected() {
        coEvery { parts.getManga.await(1L) } returns original
        coEvery { parts.getManga.await("/orig", MERGED_SOURCE_ID) } returns original.copy(favorite = true)
        shouldThrow<IllegalArgumentException> { runBlocking { merger.smartSearchMerge(incoming, 1L) } }
    }

    @Test
    fun settingsAreSaved() {
        runBlocking { merger.updateMergeSettings(emptyList()) }
        coVerify(exactly = 0) { parts.updateMergedSettings.awaitAll(any()) }
        runBlocking { merger.updateMergeSettings(listOf(reference(8L))) }
        coVerify { parts.updateMergedSettings.awaitAll(match { it.single().id == 3L }) }
        runBlocking { merger.deleteMerge(reference(8L)) }
        coVerify { parts.deleteMergeById.await(3L) }
    }

    @Test
    fun modelDelegatesToTheMerger() {
        harness.mangaFlow.value = manga(favorite = true) to emptyList()
        val model = harness.loaded()
        model.updateMergeSettings(listOf(reference(8L)))
        model.deleteMerge(reference(8L))
        coVerify(timeout = 5_000) { parts.updateMergedSettings.awaitAll(any()) }
        coVerify(timeout = 5_000) { parts.deleteMergeById.await(3L) }
    }
}
