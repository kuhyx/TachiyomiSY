package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(RobolectricTestRunner::class)
internal class MoveLibraryNonCompleteSettingMigrationTest {

    private val migration = MoveLibraryNonCompleteSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 23f
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
    fun keepsTheRestrictionByDefault() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        libraryPreferences.autoUpdateMangaRestrictions.get() shouldContain LibraryPreferences.MANGA_NON_COMPLETED
    }

    @Test
    fun dropsTheRestrictionWhenOff() = runTest {
        prefs.edit { putBoolean("pref_update_only_non_completed_key", false) }
        startBoth()
        migration(migrationContext()) shouldBe true
        libraryPreferences.autoUpdateMangaRestrictions.get() shouldNotContain LibraryPreferences.MANGA_NON_COMPLETED
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
    }
}
