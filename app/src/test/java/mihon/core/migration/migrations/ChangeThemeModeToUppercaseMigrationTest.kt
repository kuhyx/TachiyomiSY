package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class ChangeThemeModeToUppercaseMigrationTest {

    private val migration = ChangeThemeModeToUppercaseMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val uiPreferences = UiPreferences(InMemoryPreferenceStore())
    private val key = uiPreferences.themeMode.key()

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 42f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { uiPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesAnUnsetTheme() = runTest {
        prefs.edit { putString(key, "dark") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "dark"
    }

    @Test
    fun leavesAThemeMissingFromDisk() = runTest {
        uiPreferences.themeMode.set(ThemeMode.DARK)
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.contains(key) shouldBe false
    }

    @Test
    fun uppercasesTheStoredTheme() = runTest {
        uiPreferences.themeMode.set(ThemeMode.DARK)
        prefs.edit { putString(key, "dark") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "DARK"
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { uiPreferences }
        }
    }
}
