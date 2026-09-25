package mihon.core.migration.migrations

import android.app.Application
import exh.log.xLogE
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class DeleteOldEhFavoritesDatabaseMigrationTest {

    private val migration = DeleteOldEhFavoritesDatabaseMigration()

    @TempDir
    lateinit var filesDir: File

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 24f
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun deletesOldFilesAndDirs() = runTest {
        val directory = File(filesDir, "fav-sync").apply { mkdirs() }
        File(directory, "inner").writeText("x")
        val lock = File(filesDir, "fav-sync.lock").apply { writeText("l") }
        val unrelated = File(filesDir, "other").apply { writeText("o") }
        val app = mockk<Application>()
        every { app.filesDir } returns filesDir
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        directory.exists() shouldBe false
        lock.exists() shouldBe false
        unrelated.exists() shouldBe true
    }

    @Test
    fun logsUnreadableFilesDir() = runTest {
        val failure = IllegalStateException("no files dir")
        val app = mockk<Application>()
        every { app.filesDir } throws failure
        mockkStatic("exh.log.LoggingThrowablesKt")
        every { migration.xLogE(any<String>(), any<Throwable>()) } returns Unit
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { migration.xLogE("Failed to delete old favorites database", failure) }
    }
}
