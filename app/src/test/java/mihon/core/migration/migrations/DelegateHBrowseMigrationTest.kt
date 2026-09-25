package mihon.core.migration.migrations

import eu.kanade.domain.manga.interactor.UpdateManga
import exh.source.HBROWSE_SOURCE_ID
import exh.source.LEGACY_HBROWSE_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.domain.manga.interactor.GetMangaBySource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate

internal class DelegateHBrowseMigrationTest {

    private val migration = DelegateHBrowseMigration()
    private val getMangaBySource = mockk<GetMangaBySource>()
    private val updateManga = mockk<UpdateManga>()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 4f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { getMangaBySource } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { updateManga } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun rewritesUrlsWithoutDatabase() = runTest {
        val manga = Manga.create().copy(id = 5, url = "/manga/x", source = HBROWSE_SOURCE_ID)
        coEvery { getMangaBySource.await(HBROWSE_SOURCE_ID) } returns listOf(manga)
        coEvery { updateManga.awaitAll(any()) } returns true
        startMigrationKoin {
            single { getMangaBySource }
            single { updateManga }
        }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { updateManga.awaitAll(listOf(MangaUpdate(id = 5, url = "/manga/x/c00001/"))) }
    }

    @Test
    fun rewritesTheSourceIdToo() = runTest {
        val queries = mockk<EhQueries>(relaxed = true)
        val database = mockk<Database> { every { ehQueries } returns queries }
        coEvery { getMangaBySource.await(HBROWSE_SOURCE_ID) } returns emptyList()
        coEvery { updateManga.awaitAll(emptyList()) } returns true
        startMigrationKoin {
            single { getMangaBySource }
            single { updateManga }
            single { database }
        }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { queries.migrateSource(HBROWSE_SOURCE_ID, LEGACY_HBROWSE_SOURCE_ID) }
    }
}
