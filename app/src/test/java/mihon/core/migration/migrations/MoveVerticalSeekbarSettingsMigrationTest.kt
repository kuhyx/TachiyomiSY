package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveVerticalSeekbarSettingsMigrationTest {

    private val migration = MoveVerticalSeekbarSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val readerPreferences = ReaderPreferences(InMemoryPreferenceStore())
    private val onLeftKey = readerPreferences.verticalNavigatorOnLeft.key()

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 77f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { readerPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesDefaultsAlone() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.contains("pref_webtoon_vertical_navigator") shouldBe false
        prefs.contains(onLeftKey) shouldBe false
    }

    @Test
    fun movesBothSeekbarSettings() = runTest {
        prefs.edit {
            putBoolean("pref_force_horz_seekbar", true)
            putBoolean("pref_left_handed_vertical_seekbar", true)
        }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getBoolean("pref_webtoon_vertical_navigator", true) shouldBe false
        prefs.getBoolean(onLeftKey, false) shouldBe true
        prefs.contains("pref_force_horz_seekbar") shouldBe false
        prefs.contains("pref_left_handed_vertical_seekbar") shouldBe false
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { readerPreferences }
        }
    }
}
