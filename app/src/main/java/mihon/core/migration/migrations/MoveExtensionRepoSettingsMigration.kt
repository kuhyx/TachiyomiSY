package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.source.service.SourcePreferences
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 60f

internal class MoveExtensionRepoSettingsMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val preferenceStore = migrationContext.get<PreferenceStore>()
        val sourcePreferences = migrationContext.get<SourcePreferences>()
        if (context == null || preferenceStore == null || sourcePreferences == null) return false
        withIOContext { migrate(context, preferenceStore, sourcePreferences) }
        return true
    }

    private fun migrate(context: Application, preferenceStore: PreferenceStore, sourcePreferences: SourcePreferences) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        sourcePreferences.extensionRepos.getAndSet {
            it.map { "https://raw.githubusercontent.com/$it/repo" }.toSet()
        }
        MigrateUtils.replacePreferences(
            preferenceStore = preferenceStore,
            filterPredicate = { it.key.startsWith("pref_mangasync_") || it.key.startsWith("track_token_") },
            newKey = { Preference.privateKey(it) },
        )
        prefs.edit {
            remove(Preference.appStateKey("trusted_signatures"))
        }
    }
}
