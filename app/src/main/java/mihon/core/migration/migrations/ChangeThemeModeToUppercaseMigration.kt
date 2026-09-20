package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.ui.UiPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 42f

internal class ChangeThemeModeToUppercaseMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val uiPreferences = migrationContext.get<UiPreferences>()
        if (context == null || uiPreferences == null) return false
        withIOContext { migrate(context, uiPreferences) }
        return true
    }

    private fun migrate(context: Application, uiPreferences: UiPreferences) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (uiPreferences.themeMode.isSet()) {
            val themeMode = prefs.getString(uiPreferences.themeMode.key(), null)
            if (themeMode != null) {
                prefs.edit { putString(uiPreferences.themeMode.key(), themeMode.uppercase()) }
            }
        }
    }
}
