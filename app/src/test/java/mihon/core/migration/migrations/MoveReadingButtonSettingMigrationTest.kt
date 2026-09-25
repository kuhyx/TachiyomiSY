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
internal class MoveReadingButtonSettingMigrationTest {

    private val migration = MoveReadingButtonSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 43f
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
    fun leavesTheButtonHidden() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        libraryPreferences.showContinueReadingButton.isSet() shouldBe false
    }

    @Test
    fun movesTheButtonSetting() = runTest {
        prefs.edit { putBoolean("start_reading_button", true) }
        startBoth()
        migration(migrationContext()) shouldBe true
        libraryPreferences.showContinueReadingButton.get() shouldBe true
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
    }
}
