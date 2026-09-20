package mihon.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 27f

internal class MoveSecureScreenSettingMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val securityPreferences = migrationContext.get<SecurityPreferences>()
        if (context == null || securityPreferences == null) return false
        withIOContext { migrate(context, securityPreferences) }
        return true
    }

    private fun migrate(context: Application, securityPreferences: SecurityPreferences) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val oldSecureScreen = prefs.getBoolean("secure_screen", false)
        if (oldSecureScreen) {
            securityPreferences.secureScreen.set(SecurityPreferences.SecureScreenMode.ALWAYS)
        }
    }
}
