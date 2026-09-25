package exh.debug

import exh.metadata.sql.models.SearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.data.Saved_searchQueries
import tachiyomi.domain.manga.interactor.GetAllManga
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetSearchMetadata
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository

internal class DebugDatabaseFunctionsTest {
    private val queries = mockk<EhQueries>(relaxed = true)
    private val savedSearchQueries = mockk<Saved_searchQueries>(relaxed = true)
    private val database = mockk<Database> {
        every { ehQueries } returns queries
        every { saved_searchQueries } returns savedSearchQueries
    }
    private val getFavorites = mockk<GetFavorites>()
    private val getSearchMetadata = mockk<GetSearchMetadata>()
    private val getAllManga = mockk<GetAllManga>()

    @BeforeEach
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { database }
                    single { getFavorites }
                    single { getSearchMetadata }
                    single { getAllManga }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private object NoCustomInfo : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = null
        override fun set(mangaInfo: CustomMangaInfo) = Unit
    }

    @Test
    fun countsComeFromTheInteractors() {
        val favourite = Manga.create().copy(id = 1, favorite = true)
        val other = Manga.create().copy(id = 2)
        coEvery { getFavorites.await() } returns listOf(favourite)
        val metadataOnly = Manga.create().copy(id = 2, favorite = true)
        coEvery { getAllManga.await() } returns listOf(favourite, other, metadataOnly)
        coEvery { getSearchMetadata.await() } returns emptyList()
        coEvery { getSearchMetadata.await(1L) } returns null
        coEvery { getSearchMetadata.await(2L) } returns SearchMetadata(2, null, "{}", null, 0)
        DebugDatabaseFunctions.countMangaInDatabaseInLibrary() shouldBe 1
        DebugDatabaseFunctions.countMangaNotInLibrary() shouldBe 1
        DebugDatabaseFunctions.countMangaInDatabase() shouldBe 3
        DebugDatabaseFunctions.countMetadataInDatabase() shouldBe 0
        DebugDatabaseFunctions.countLibraryMissingMetadata() shouldBe 1
    }

    @Test
    fun bulkEditsRunTheQueries() {
        DebugDatabaseFunctions.addAllMangaInDatabaseToLibrary()
        DebugDatabaseFunctions.clearSavedSearches()
        coVerify(exactly = 1) { queries.addAllMangaInDatabaseToLibrary() }
        coVerify(exactly = 1) { savedSearchQueries.deleteAll() }
    }
}
