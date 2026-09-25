package eu.kanade.tachiyomi.data.sync

import eu.kanade.tachiyomi.data.backup.chapterRow
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.service.manga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.data.manga.MangaMapper
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class SyncManagerDiffTest {

    private val harness = SyncManagerHarness()
    private val local by lazy { MangaMapper.mapManga(mangasRow(id = 1L, url = "r", version = 1L)) }

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun identicalMangaIsSame() = runTest {
        SyncManager(harness.context).isMangaDifferent(local, manga("r", version = 1L)) shouldBe false
    }

    @Test
    fun mangaDiffersByChapters() = runTest {
        every { harness.chapters.getChaptersByMangaId(1L, 0L) } returns fakeQuery(listOf(chapterRow(url = "c")))
        SyncManager(harness.context).isMangaDifferent(local, manga("r", version = 1L)) shouldBe true
    }

    @Test
    fun mangaDiffersByVersion() = runTest {
        SyncManager(harness.context).isMangaDifferent(local, manga("r", version = 2L)) shouldBe true
    }

    @Test
    fun mangaDiffersByCategories() = runTest {
        coEvery { harness.getCategories.await(1L) } returns listOf(Category(id = 1, name = "A", order = 3, flags = 0))
        val manager = SyncManager(harness.context)
        manager.isMangaDifferent(local, manga("r", version = 1L, categories = listOf(4L))) shouldBe true
        manager.isMangaDifferent(local, manga("r", version = 1L, categories = listOf(3L))) shouldBe false
    }

    @Test
    fun changedFavoriteIsKept() = runTest {
        every { harness.mangas.getAllManga() } returns fakeQuery(listOf(mangasRow(id = 1L, url = "r")))
        val remote = Backup(backupManga = listOf(manga("r", version = 5L)))
        val (favorites, nonFavorites) = SyncManager(harness.context).filterFavoritesAndNonFavorites(remote)
        favorites.map { it.url } shouldBe listOf("r")
        nonFavorites shouldBe emptyList()
        harness.logged.any { it.startsWith("Filtering completed in 0m 0s. Favorites found: 1") } shouldBe true
    }

    @Test
    fun onlyFlippedNonFavoritesWrite() = runTest {
        every { harness.mangas.getAllManga() } returns fakeQuery(
            listOf(mangasRow(id = 1L, url = "same", favorite = false), mangasRow(id = 2L, url = "flip")),
        )
        SyncManager(harness.context).updateNonFavorites(
            listOf(manga("same", favorite = false), manga("flip", favorite = false), manga("gone", favorite = false)),
        )
        harness.verifyFavoriteWritten(mangaId = 2L, favorite = false)
        harness.verifyFavoriteWritten(mangaId = 1L, favorite = false, times = 0)
    }

    @Test
    fun libraryQueriesAreMapped() = runTest {
        every { harness.mangas.getAllManga() } returns fakeQuery(listOf(mangasRow(id = 1L, url = "a")))
        every { harness.mangas.getMangasWithFavoriteTimestamp() } returns fakeQuery(listOf(favoriteRow(2L, "b")))
        val manager = SyncManager(harness.context)
        manager.getAllMangaFromDB().map { it.url } shouldBe listOf("a")
        manager.getAllMangaThatNeedsSync().map { it.url } shouldBe listOf("b")
    }
}
