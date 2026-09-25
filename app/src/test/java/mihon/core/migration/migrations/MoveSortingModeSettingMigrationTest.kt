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
internal class MoveSortingModeSettingMigrationTest {

    private val migration = MoveSortingModeSettingMigration()
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
        migration.version shouldBe 39f
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
    fun leavesAnUnsetSort() = runTest {
        prefs.edit { putString("library_sorting_ascending", "DESCENDING") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.contains(key) shouldBe false
        prefs.getString("library_sorting_ascending", null) shouldBe "DESCENDING"
    }

    @Test
    fun joinsSortAndDefaultDirection() = runTest {
        prefs.edit { putString(key, "ALPHABETICAL") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "ALPHABETICAL,ASCENDING"
    }

    @Test
    fun joinsSortAndStoredDirection() = runTest {
        prefs.edit {
            putString(key, "LAST_READ")
            putString("library_sorting_ascending", "DESCENDING")
        }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "LAST_READ,DESCENDING"
        prefs.contains("library_sorting_ascending") shouldBe false
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
    }
}
