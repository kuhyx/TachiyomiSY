package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.navigationModePager
import eu.kanade.tachiyomi.ui.reader.setting.navigationModeWebtoon
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveReaderTapSettingMigrationTest {

    private val migration = MoveReaderTapSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val readerPreferences = ReaderPreferences(AndroidPreferenceStore(app))

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 32f
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
    fun keepsTapZonesWhenTheyWereOn() = runTest {
        prefs.edit { putBoolean("reader_tap", true) }
        startBoth()
        migration(migrationContext()) shouldBe true
        readerPreferences.navigationModePager.isSet() shouldBe false
        readerPreferences.navigationModeWebtoon.isSet() shouldBe false
    }

    @Test
    fun disablesTapZonesWhenOff() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        readerPreferences.navigationModePager.get() shouldBe 5
        readerPreferences.navigationModeWebtoon.get() shouldBe 5
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { readerPreferences }
        }
    }
}
