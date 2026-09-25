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
internal class MoveLibrarySortingSettingsMigrationTest {

    private val migration = MoveLibrarySortingSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())
    private val key = libraryPreferences.sortingMode.key()

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 20f
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
    fun defaultsToAlphabeticalAsc() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "ALPHABETICAL"
        prefs.getString("library_sorting_ascending", null) shouldBe "ASCENDING"
    }

    @Test
    fun mapsTheLegacyIndexAndDirection() = runTest {
        prefs.edit {
            putInt(key, 8)
            putBoolean("library_sorting_ascending", false)
        }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "DATE_ADDED"
        prefs.getString("library_sorting_ascending", null) shouldBe "DESCENDING"
    }

    @Test
    fun fallsBackForUnknownIndex() = runTest {
        for (index in listOf(42, -1)) {
            prefs.edit {
                clear()
                putInt(key, index)
            }
            startBoth()
            migration(migrationContext()) shouldBe true
            prefs.getString(key, null) shouldBe "ALPHABETICAL"
            stopMigrationKoin()
        }
    }

    @Test
    fun skipsAnAlreadyMigratedSort() = runTest {
        prefs.edit { putString(key, "LAST_READ") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "LAST_READ"
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
    }
}
