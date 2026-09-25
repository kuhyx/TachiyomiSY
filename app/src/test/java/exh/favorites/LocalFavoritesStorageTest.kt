package exh.favorites

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFavoriteEntries
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.InsertFavoriteEntries
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository

internal class LocalFavoritesStorageTest {

    private val favorites = listOf(
        libraryManga(id = 1, url = "/g/gid/token"),
        // an alias for gid2/token2
        libraryManga(id = 3, url = "/g/gid3/token3"),
        // add this one to library
        libraryManga(id = 3, url = "/g/gid4/token4"),
    )

    private val categories = listOf(Category(id = 1, name = "a", order = 1, flags = 0))

    private val deleteFavoriteEntries = mockk<DeleteFavoriteEntries>(relaxed = true)
    private val insertFavoriteEntries = mockk<InsertFavoriteEntries>(relaxed = true)

    private val favoriteEntries = listOf(
        FavoriteEntry(gid = "gid", token = "token", title = "a", category = 0),
        FavoriteEntry(gid = "gid2", token = "token2", title = "a", category = 0),
        // the alias for gid2/token2
        FavoriteEntry(
            gid =
            "gid2",
            token = "token2", title = "a", category = 0, otherGid = "gid3", otherToken = "token3",
        ),
        // removed on remote and local
        FavoriteEntry(gid = "gid6", token = "token6", title = "a", category = 0),
    )

    /** gid4 is in the library but not in the stored entries; gid6 is stored but no longer in the library. */
    @Test
    fun dbDiffAddsNewRemovesStale() = runBlocking<Unit> {
        val (added, removed) = storage().getChangedDbEntries()
        added.shouldForAll { it.gid == "gid4" && it.token == "token4" }
        removed.shouldForAll { it.gid == "gid6" && it.token == "token6" }
    }

    /** gid3 is only an alias of gid2, so it is neither added nor removed; gid5 is new on the remote. */
    @Test
    fun remoteDiffResolvesAliases() = runBlocking<Unit> {
        val remote = listOf("/g/gid/token", "/g/gid2/token2", "/g/gid5/token5").map { url ->
            EHentai.ParsedManga(0, SManga(url, "a"), EHentaiSearchMetadata())
        }
        val (remoteAdded, remoteRemoved) = storage().getChangedRemoteEntries(remote)
        remoteAdded.shouldForAll { it.gid == "gid5" && it.token == "token5" }
        remoteRemoved.shouldForAll { it.gid == "gid6" && it.token == "token6" }
    }

    private fun storage(dbFavorites: List<Manga> = favorites): LocalFavoritesStorage {
        val getFavorites = mockk<GetFavorites>()
        coEvery { getFavorites.await() } returns dbFavorites
        val getCategories = mockk<GetCategories>()
        coEvery { getCategories.await() } returns categories
        coEvery { getCategories.await(any()) } returns categories
        coEvery { getCategories.await(UNCATEGORISED_ID) } returns emptyList()
        val getFavoriteEntries = mockk<GetFavoriteEntries>()
        coEvery { getFavoriteEntries.await() } returns favoriteEntries
        return LocalFavoritesStorage(
            getFavorites = getFavorites,
            getCategories = getCategories,
            deleteFavoriteEntries = deleteFavoriteEntries,
            getFavoriteEntries = getFavoriteEntries,
            insertFavoriteEntries = insertFavoriteEntries,
        )
    }

    /** Non-favourites, non-EH entries, uncategorised entries and categories past the tenth slot are skipped. */
    @Test
    fun snapshotKeepsSyncableEntries() = runBlocking<Unit> {
        val many = (0..MAX_INDEX + 1).map {
            Category(id = it.toLong() + 10, name = "c$it", order = it.toLong(), flags = 0)
        }
        val getCategories = mockk<GetCategories>()
        coEvery { getCategories.await() } returns many
        coEvery { getCategories.await(any()) } returns listOf(many.last())
        coEvery { getCategories.await(1L) } returns listOf(many.first())
        coEvery { getCategories.await(UNCATEGORISED_ID) } returns emptyList()
        val getFavorites = mockk<GetFavorites>()
        coEvery { getFavorites.await() } returns listOf(
            libraryManga(id = 1, url = "/g/gid/token"),
            libraryManga(id = 2, url = "/g/gid2/token2"),
            libraryManga(id = UNCATEGORISED_ID, url = "/g/gid3/token3"),
            libraryManga(id = 4, url = "/g/gid4/token4").copy(favorite = false),
            libraryManga(id = 5, url = "/g/gid5/token5").copy(source = 1L),
        )
        val storage = LocalFavoritesStorage(
            getFavorites = getFavorites,
            getCategories = getCategories,
            deleteFavoriteEntries = deleteFavoriteEntries,
            getFavoriteEntries = mockk(),
            insertFavoriteEntries = insertFavoriteEntries,
        )
        storage.snapshotEntries()
        coVerify(exactly = 1) { deleteFavoriteEntries.await() }
        val inserted = slot<List<FavoriteEntry>>()
        coVerify(exactly = 1) { insertFavoriteEntries.await(capture(inserted)) }
        inserted.captured.map { it.gid to it.category } shouldContainExactly listOf("gid" to 0)
    }

    @Test
    fun clearSnapshotsDeletesEntries() = runBlocking<Unit> {
        storage().clearSnapshots()
        coVerify(exactly = 1) { deleteFavoriteEntries.await() }
        coVerify(exactly = 0) { insertFavoriteEntries.await(any()) }
    }

    @Test
    fun urlEqualsUsesEitherIdentity() {
        val plain = FavoriteEntry(gid = "a", token = "t", title = "x", category = 0)
        val moved = FavoriteEntry(gid = "b", token = "u", title = "x", category = 0, otherGid = "a", otherToken = "t")
        val halfMoved = FavoriteEntry(gid = "c", token = "v", title = "x", category = 0, otherGid = "a")
        plain.urlEquals(moved) shouldBe true
        moved.urlEquals(plain) shouldBe true
        plain.urlEquals(halfMoved) shouldBe false
        halfMoved.urlEquals(plain) shouldBe false
        ChangeSet(listOf(plain), emptyList()).added.single() shouldBe plain
        LocalFavoritesStorage.MAX_CATEGORIES shouldBe MAX_INDEX
    }

    @Test
    fun remoteSkipsHighCategories() = runBlocking<Unit> {
        val remote = listOf(0, MAX_INDEX + 1).map { fav ->
            EHentai.ParsedManga(fav, SManga("/g/gid9/token9", "a"), EHentaiSearchMetadata())
        }
        val (added, _) = storage().getChangedRemoteEntries(remote)
        added.map { it.category } shouldContainExactly listOf(0)
    }

    private fun libraryManga(id: Long, url: String): Manga =
        Manga.create().copy(id = id, favorite = true, source = EXH_SOURCE_ID, url = url)

    companion object {
        private const val UNCATEGORISED_ID = 3L
        private const val MAX_INDEX = 9

        @JvmStatic
        @AfterAll
        fun after() = stopKoin()

        @JvmStatic
        @BeforeAll
        fun before() {
            stopKoin()
            startKoin {
                modules(
                    module {
                        single {
                            GetCustomMangaInfo(
                                object : CustomMangaRepository {
                                    override fun get(mangaId: Long) = null
                                    override fun set(mangaInfo: CustomMangaInfo) = Unit
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}
