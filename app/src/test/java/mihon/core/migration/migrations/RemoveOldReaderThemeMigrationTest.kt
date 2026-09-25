package mihon.core.migration.migrations

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class RemoveOldReaderThemeMigrationTest {

    private val migration = RemoveOldReaderThemeMigration()
    private val preferences = ReaderPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 18f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsOtherThemes() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.readerTheme.set(2)
        migration(migrationContext()) shouldBe true
        preferences.readerTheme.get() shouldBe 2
    }

    @Test
    fun foldsRemovedThemeIntoAutomatic() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.readerTheme.set(4)
        migration(migrationContext()) shouldBe true
        preferences.readerTheme.get() shouldBe 3
    }
}
