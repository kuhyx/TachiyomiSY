package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ClearBrokenPagePreviewCacheMigrationTest {

    private val migration = ClearBrokenPagePreviewCacheMigration()
    private val cache = mockk<PagePreviewCache> { every { clear() } returns 0 }

    @TempDir
    lateinit var cacheDir: File

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 58f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app() } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { cache } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun clearsWhenNoCacheDirExists() = runTest {
        startMigrationKoin {
            single { app() }
            single { cache }
        }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { cache.clear() }
    }

    @Test
    fun deletesEverythingButTheJournal() = runTest {
        val dir = File(cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).apply { mkdirs() }
        val journal = File(dir, "journal").apply { writeText("j") }
        val journalTmp = File(dir, "journal.tmp").apply { writeText("t") }
        val entry = File(dir, "abc.0").apply { writeText("x") }
        startMigrationKoin {
            single { app() }
            single { cache }
        }
        migration(migrationContext()) shouldBe true
        journal.exists() shouldBe true
        journalTmp.exists() shouldBe true
        entry.exists() shouldBe false
    }

    // A non-empty directory cannot be deleted, whoever runs the test.
    @Test
    fun keepsWhatCannotBeDeleted() = runTest {
        val dir = File(cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).apply { mkdirs() }
        val stuck = File(dir, "stuck").apply { mkdirs() }
        File(stuck, "inner").writeText("x")
        startMigrationKoin {
            single { app() }
            single { cache }
        }
        migration(migrationContext()) shouldBe true
        stuck.exists() shouldBe true
    }

    private fun app(): Application {
        val app = mockk<Application>()
        every { app.cacheDir } returns cacheDir
        return app
    }
}
