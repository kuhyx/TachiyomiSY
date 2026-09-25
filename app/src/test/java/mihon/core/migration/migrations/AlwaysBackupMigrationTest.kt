package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.backup.service.BackupPreferences

internal class AlwaysBackupMigrationTest {

    private val migration = AlwaysBackupMigration()
    private val preferences = BackupPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 40f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsNonZeroInterval() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.backupInterval.set(24)
        migration(migrationContext()) shouldBe true
        preferences.backupInterval.get() shouldBe 24
    }

    @Test
    fun replacesDisabledBackup() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.backupInterval.set(0)
        migration(migrationContext()) shouldBe true
        preferences.backupInterval.get() shouldBe 12
    }
}
