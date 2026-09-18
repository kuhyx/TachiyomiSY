package tachiyomi.domain.backup.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

internal class BackupPreferencesTest {

    private val preferences = BackupPreferences(InMemoryPreferenceStore())

    @Test
    fun intervalDefaultsTo12Hours() {
        preferences.backupInterval.key() shouldBe "backup_interval"
        preferences.backupInterval.get() shouldBe 12
    }

    @Test
    fun lastBackupIsAppState() {
        Preference.isAppState(preferences.lastAutoBackupTimestamp.key()) shouldBe true
        preferences.lastAutoBackupTimestamp.get() shouldBe 0L
    }

    @Test
    fun preferencesHoldValues() {
        preferences.backupInterval.set(24)
        preferences.lastAutoBackupTimestamp.set(5L)

        preferences.backupInterval.get() shouldBe 24
        preferences.lastAutoBackupTimestamp.get() shouldBe 5L
    }
}
