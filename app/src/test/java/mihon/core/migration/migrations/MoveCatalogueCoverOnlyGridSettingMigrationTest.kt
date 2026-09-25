package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MoveCatalogueCoverOnlyGridSettingMigrationTest {

    private val migration = MoveCatalogueCoverOnlyGridSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 29f
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesOtherDisplayModes() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.contains("pref_display_mode_catalogue") shouldBe false
    }

    @Test
    fun renamesNoTitleGrid() = runTest {
        prefs.edit { putString("pref_display_mode_catalogue", "NO_TITLE_GRID") }
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.getString("pref_display_mode_catalogue", null) shouldBe "COMPACT_GRID"
    }
}
