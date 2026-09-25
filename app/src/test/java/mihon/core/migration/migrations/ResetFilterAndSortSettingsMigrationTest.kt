package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(RobolectricTestRunner::class)
internal class ResetFilterAndSortSettingsMigrationTest {

    private val migration = ResetFilterAndSortSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 41f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { libraryPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesUnsetPreferences() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.all.isEmpty() shouldBe true
    }

    @Test
    fun widensStoredIntsToLongs() = runTest {
        val readKey = libraryPreferences.filterChapterByRead.key()
        val sortKey = libraryPreferences.sortChapterByAscendingOrDescending.key()
        prefs.edit {
            putInt(readKey, 2)
            putInt(sortKey, 1)
        }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getLong(readKey, -1) shouldBe 2L
        prefs.getLong(sortKey, -1) shouldBe 1L
        prefs.contains(libraryPreferences.filterChapterByDownloaded.key()) shouldBe false
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
    }
}
