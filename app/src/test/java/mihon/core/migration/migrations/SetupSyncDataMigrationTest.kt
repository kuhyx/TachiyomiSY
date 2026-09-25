package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.migration.Migration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class SetupSyncDataMigrationTest {

    private val migration = SetupSyncDataMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun runsAlways() {
        migration.version shouldBe Migration.ALWAYS
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun schedulesTheJob() = runTest {
        val app = mockk<Application>()
        mockkObject(SyncDataJob.Companion)
        every { SyncDataJob.setupTask(app, any()) } returns Unit
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { SyncDataJob.setupTask(app, null) }
    }
}
