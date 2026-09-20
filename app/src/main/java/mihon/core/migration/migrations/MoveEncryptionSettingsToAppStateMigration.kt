package mihon.core.migration.migrations

import android.app.Application
import android.widget.Toast
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.util.system.toast
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext

private const val VERSION = 66f

internal class MoveEncryptionSettingsToAppStateMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val preferenceStore = migrationContext.get<PreferenceStore>()
        if (context == null || preferenceStore == null) return false
        withIOContext { migrate(context, preferenceStore) }
        return true
    }

    private suspend fun migrate(context: Application, preferenceStore: PreferenceStore) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBoolean(Preference.privateKey("encrypt_database"), false)) {
            withUIContext {
                context.toast(
                    "Restart the app to load your encrypted library",
                    Toast.LENGTH_LONG,
                )
            }
        }

        val appStatePrefsToReplace = listOf(
            "__PRIVATE_sql_password",
            "__PRIVATE_encrypt_database",
            "__PRIVATE_cbz_password",
        )

        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key in appStatePrefsToReplace },
            newKey = { Preference.appStateKey(it.replace("__PRIVATE_", "").trim()) },
        )
    }
}
