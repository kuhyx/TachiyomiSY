package mihon.core.migration.migrations

import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.LEGACY_NHENTAI_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.EhQueries

internal class DelegateNHentaiMigrationTest {

    private val migration = DelegateNHentaiMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 6f
    }

    @Test
    fun succeedsWithoutDatabase() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe true
    }

    @Test
    fun rewritesTheSourceId() = runTest {
        val queries = mockk<EhQueries>(relaxed = true)
        val database = mockk<Database> { every { ehQueries } returns queries }
        startMigrationKoin { single { database } }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { queries.migrateSource(NHentai.otherId, LEGACY_NHENTAI_SOURCE_ID) }
    }
}
