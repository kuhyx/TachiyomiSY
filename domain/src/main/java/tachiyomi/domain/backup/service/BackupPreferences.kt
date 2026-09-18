package tachiyomi.domain.backup.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Settings of automatic backups. */
public class BackupPreferences(
    preferenceStore: PreferenceStore,
) {

    /** Hours between automatic backups; 0 disables them. */
    public val backupInterval: Preference<Int> = preferenceStore.getInt("backup_interval", DEFAULT_INTERVAL_HOURS)

    /** Epoch millis of the last automatic backup; app state. */
    public val lastAutoBackupTimestamp: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("last_auto_backup_timestamp"),
        0L,
    )

    private companion object {
        const val DEFAULT_INTERVAL_HOURS = 12
    }
}
