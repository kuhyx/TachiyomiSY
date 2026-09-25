package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class MoveSettingsToPrivateOrAppStateMigrationTest {

    private val migration = MoveSettingsToPrivateOrAppStateMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val store: PreferenceStore = AndroidPreferenceStore(app)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 59f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { store } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun movesKeysAndClearsTheCache() = runTest {
        val cached = File(app.cacheDir, "index").apply { writeText("x") }
        prefs.edit {
            putBoolean("pref_download_only", true)
            putLong("last_catalogue_source", 7L)
            putInt("library_unseen_updates_count", 3)
            putString("storage_dir", "/sd")
            putFloat("eh_auto_update_stats", 1.5f)
            putString("sql_password", "secret")
            putStringSet("eh_settingsKey", setOf("k"))
            putString("unrelated", "kept")
        }
        startMigrationKoin {
            single { app }
            single { store }
        }
        migration(migrationContext()) shouldBe true
        prefs.getBoolean(Preference.appStateKey("pref_download_only"), false) shouldBe true
        prefs.getLong(Preference.appStateKey("last_catalogue_source"), 0) shouldBe 7L
        prefs.getInt(Preference.appStateKey("library_unseen_updates_count"), 0) shouldBe 3
        prefs.getString(Preference.appStateKey("storage_dir"), null) shouldBe "/sd"
        prefs.getFloat(Preference.appStateKey("eh_auto_update_stats"), 0f) shouldBe 1.5f
        prefs.getString(Preference.privateKey("sql_password"), null) shouldBe "secret"
        prefs.getStringSet(Preference.privateKey("eh_settingsKey"), null) shouldBe setOf("k")
        prefs.getString("unrelated", null) shouldBe "kept"
        prefs.contains("pref_download_only") shouldBe false
        prefs.contains("sql_password") shouldBe false
        cached.exists() shouldBe false
    }
}
