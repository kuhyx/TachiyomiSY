package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.archiveReaderMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveCacheToDiskSettingMigrationTest {

    private val migration = MoveCacheToDiskSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val readerPreferences = ReaderPreferences(AndroidPreferenceStore(app))

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 66f
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
    fun leavesTheDefaultMode() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        readerPreferences.archiveReaderMode.isSet() shouldBe false
    }

    @Test
    fun movesTheCacheToDiskSetting() = runTest {
        prefs.edit { putBoolean("cache_archive_manga_on_disk", true) }
        startBoth()
        migration(migrationContext()) shouldBe true
        readerPreferences.archiveReaderMode.get() shouldBe ReaderPreferences.ArchiveReaderMode.CACHE_TO_DISK
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { readerPreferences }
        }
    }
}
