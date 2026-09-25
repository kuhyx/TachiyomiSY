package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.EhQueries

internal class DeleteOldMangaDexTracksMigrationTest {

    private val migration = DeleteOldMangaDexTracksMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 17f
    }

    @Test
    fun failsWithoutDatabase() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun deletesTheOldTracks() = runTest {
        val queries = mockk<EhQueries>(relaxed = true)
        val database = mockk<Database> { every { ehQueries } returns queries }
        startMigrationKoin { single { database } }
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) { queries.deleteBySyncId(6L) }
    }
}
