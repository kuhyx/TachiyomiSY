package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import exh.source.MERGED_SOURCE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.MangaUpdate

@RunWith(RobolectricTestRunner::class)
internal class LibraryRemoveTest {
    private val harness = LibraryHarness()
    private val http = mockk<HttpSource>(relaxed = true) { every { id } returns 20L }
    private val plain = mockk<Source>(relaxed = true) { every { id } returns 22L }
    private val merged = mockk<MergedSource>(relaxed = true)
    private val online by lazy { manga(1).copy(source = 20) }
    private val unknown by lazy { manga(2).copy(source = 21) }
    private val mergedEntry by lazy { manga(3).copy(source = MERGED_SOURCE_ID) }
    private val parts by lazy { listOf(manga(4).copy(source = 20), manga(5).copy(source = 22)) }

    @Before
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        every { harness.sourceManager.get(20L) } returns http
        every { harness.sourceManager.get(21L) } returns null
        every { harness.sourceManager.get(MERGED_SOURCE_ID) } returns merged
        every { harness.sourceManager.getOrStub(20L) } returns http
        every { harness.sourceManager.getOrStub(22L) } returns plain
        coEvery { harness.getMergedMangaById.await(3L) } returns parts
    }

    @After
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    @Test
    fun removeFromLibraryOnly() {
        harness.model().removeMangas(listOf(online), deleteFromLibrary = true, deleteChapters = false)
        coVerify(timeout = WAIT) { harness.updateManga.awaitAll(listOf(MangaUpdate(id = 1, favorite = false))) }
        verify { harness.coverCache.deleteFromCache(online, true) }
        coVerify(exactly = 0) { harness.getMergedMangaById.await(any()) }
    }

    @Test
    fun deleteChaptersPerSource() {
        val entries = listOf(online, unknown, mergedEntry)
        harness.model().removeMangas(entries, deleteFromLibrary = false, deleteChapters = true)
        // Only entries on an HttpSource get their download directory deleted.
        val provider = harness.downloadManager.provider
        verify(timeout = WAIT) { provider.findMangaDir(online.ogTitle, http) }
        verify(timeout = WAIT) { provider.findMangaDir(parts[0].ogTitle, http) }
        verify(exactly = 0) { provider.findMangaDir(parts[1].ogTitle, any()) }
        verify(exactly = 0) { provider.findMangaDir(unknown.ogTitle, any()) }
        coVerify(exactly = 0) { harness.updateManga.awaitAll(any()) }
    }

    @Test
    fun categoriesAreAddedAndRemoved() {
        coEvery { harness.getCategories.await(1L) } returns listOf(category(1), category(2))
        harness.model().setMangaCategories(listOf(online), addCategories = listOf(3L), removeCategories = listOf(2L))
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(1L, listOf(1L, 3L)) }
    }

    private fun category(id: Long) = Category(id = id, name = "C$id", order = id, flags = 0)

    private companion object {
        const val WAIT = 5_000L
    }
}
