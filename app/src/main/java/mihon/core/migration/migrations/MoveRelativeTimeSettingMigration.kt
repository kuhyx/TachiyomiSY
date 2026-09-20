package mihon.core.migration.migrations

import eu.kanade.domain.ui.UiPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 57f

private const val DEFAULT_RELATIVE_TIME_DAYS = 7

internal class MoveRelativeTimeSettingMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val preferenceStore = migrationContext.get<PreferenceStore>()
        val uiPreferences = migrationContext.get<UiPreferences>()
        if (preferenceStore == null || uiPreferences == null) return false
        withIOContext { migrate(preferenceStore, uiPreferences) }
        return true
    }

    private fun migrate(preferenceStore: PreferenceStore, uiPreferences: UiPreferences) {
        val pref = preferenceStore.getInt("relative_time", DEFAULT_RELATIVE_TIME_DAYS)
        if (pref.get() == 0) {
            uiPreferences.relativeTime.set(false)
        }
    }
}
