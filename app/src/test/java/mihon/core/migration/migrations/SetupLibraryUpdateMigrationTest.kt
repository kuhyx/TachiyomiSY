package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.setupTask
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.migration.Migration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class SetupLibraryUpdateMigrationTest {

    private val migration = SetupLibraryUpdateMigration()

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
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { LibraryUpdateJob.setupTask(app, any()) } returns Unit
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { LibraryUpdateJob.setupTask(app, null) }
    }
}
