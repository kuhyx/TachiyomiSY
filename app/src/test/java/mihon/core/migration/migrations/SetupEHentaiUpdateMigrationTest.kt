package mihon.core.migration.migrations

import android.app.Application
import exh.eh.EHentaiUpdateWorker
import exh.eh.scheduleBackground
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

internal class SetupEHentaiUpdateMigrationTest {

    private val migration = SetupEHentaiUpdateMigration()

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
    fun schedulesTheWorker() = runTest {
        val app = mockk<Application>()
        mockkStatic("exh.eh.EHentaiUpdateSchedulingKt")
        every { EHentaiUpdateWorker.scheduleBackground(app, any(), any()) } returns Unit
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { EHentaiUpdateWorker.scheduleBackground(app, null, null) }
    }
}
