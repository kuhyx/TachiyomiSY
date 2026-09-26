package eu.kanade.tachiyomi.data.library

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

/** Pulling MangaDex follows into the library. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateJobMangaDexTest : LibraryUpdateTestBase() {

    private val preferences = SourcePreferences(store)
    private val mangaDex = mockk<MangaDex> { every { id } returns MD_ID }

    @Before
    fun setUpSync() {
        loadKoinModules(module { single { preferences } })
        preferences.mangadexSyncToLibraryIndexes.set(setOf("1"))
        coEvery { updateFromRemote(any<Manga>(), any(), any(), any(), any(), any()) } answers {
            Result.success(RemoteMangaUpdate(firstArg(), emptyList()))
        }
    }

    private fun follow(url: String, status: Int?): Pair<SManga, MangaDexSearchMetadata> =
        SManga.create().apply {
            this.url = url
            title = url.uppercase()
        } to MangaDexSearchMetadata().apply { followStatus = status }

    @Test
    fun noEnabledMangaDexDoesNothing() = runTest {
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns null
        job().syncFollows()
        coVerify(exactly = 0) { getManga.await(any<String>(), any()) }
    }

    @Test
    fun enabledMangaDexIsSynced() = runTest {
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns mangaDex
        coEvery { mangaDex.fetchAllFollows() } returns emptyList()
        job().syncFollows()
        coVerify { mangaDex.fetchAllFollows() }
    }

    @Test
    fun followsJoinTheLibrary() = runTest {
        val fresh = follow("a", 1)
        val unfavorite = follow("b", 1)
        val present = follow("c", 1)
        val skipped = listOf(follow("d", 2), follow("e", null))
        coEvery { mangaDex.fetchAllFollows() } returns listOf(fresh, unfavorite, present) + skipped
        coEvery { getManga.await("a", MD_ID) } returns null
        coEvery { networkToLocalManga(any<Manga>()) } returns manga(10L, source = MD_ID)
        coEvery { getManga.await("b", MD_ID) } returns manga(11L, source = MD_ID).copy(favorite = false)
        coEvery { getManga.await("c", MD_ID) } returns manga(12L, source = MD_ID)
        job().syncFollows(mangaDex, preferences)
        coVerify { networkToLocalManga(match<Manga> { it.url == "a" && it.favorite }) }
        coVerify(exactly = 1) { updateManga.awaitUpdateFavorite(11L, true) }
        coVerify(exactly = 3) { insertFlatMetadata.await(any<MangaDexSearchMetadata>()) }
        listOf(fresh, unfavorite, present).map { it.second.mangaId } shouldBe listOf(10L, 11L, 12L)
        coVerify(exactly = 0) { getManga.await("d", MD_ID) }
    }

    private companion object {
        const val MD_ID = 7L
    }
}
