package exh.favorites

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
import io.kotest.inspectors.shouldForAll
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFavoriteEntries
import tachiyomi.domain.manga.interactor.GetFavorites
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

    private fun storage(): LocalFavoritesStorage {
        val getFavorites = mockk<GetFavorites>()
        coEvery { getFavorites.await() } returns favorites
        val getCategories = mockk<GetCategories>()
        coEvery { getCategories.await() } returns categories
        coEvery { getCategories.await(any()) } returns categories
        val getFavoriteEntries = mockk<GetFavoriteEntries>()
        coEvery { getFavoriteEntries.await() } returns favoriteEntries
        return LocalFavoritesStorage(
            getFavorites = getFavorites,
            getCategories = getCategories,
            deleteFavoriteEntries = mockk(),
            getFavoriteEntries = getFavoriteEntries,
            insertFavoriteEntries = mockk(),
        )
    }

    private fun libraryManga(id: Long, url: String): Manga =
        Manga.create().copy(id = id, favorite = true, source = EXH_SOURCE_ID, url = url)

    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
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
