package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
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

internal class SetupBackupCreateMigrationTest {

    private val migration = SetupBackupCreateMigration()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun runsAlways() {
        migration.version shouldBe Migration.ALWAYS
        migration.isAlways shouldBe true
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun schedulesTheJob() = runTest {
        val app = mockk<Application>()
        mockkObject(BackupCreateJob.Companion)
        every { BackupCreateJob.setupTask(app) } returns Unit
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { BackupCreateJob.setupTask(app) }
    }
}
