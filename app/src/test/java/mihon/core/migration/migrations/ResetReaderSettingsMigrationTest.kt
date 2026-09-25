package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResetReaderSettingsMigrationTest {

    private val migration = ResetReaderSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 17f
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun defaultsToFreeAndFirstViewer() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.getInt("pref_default_orientation_type_key", -1) shouldBe ReaderOrientation.FREE.flagValue
        prefs.getInt("pref_default_reading_mode_key", -1) shouldBe 1
    }

    @Test
    fun mapsTheLegacyRotation() = runTest {
        prefs.edit {
            putInt("pref_rotation_type_key", 3)
            putInt("pref_default_viewer_key", 2)
        }
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.getInt("pref_default_orientation_type_key", -1) shouldBe ReaderOrientation.LANDSCAPE.flagValue
        prefs.getInt("pref_default_reading_mode_key", -1) shouldBe 2
        prefs.contains("pref_rotation_type_key") shouldBe false
        prefs.contains("pref_default_viewer_key") shouldBe false
    }

    @Test
    fun fallsBackForUnknownRotation() = runTest {
        for (rotation in listOf(9, -1)) {
            prefs.edit { putInt("pref_rotation_type_key", rotation) }
            startMigrationKoin { single { app } }
            migration(migrationContext()) shouldBe true
            prefs.getInt("pref_default_orientation_type_key", -1) shouldBe ReaderOrientation.FREE.flagValue
            stopMigrationKoin()
        }
    }
}
