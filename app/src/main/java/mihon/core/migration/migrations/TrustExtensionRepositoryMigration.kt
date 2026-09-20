package mihon.core.migration.migrations

import eu.kanade.domain.source.service.SourcePreferences
import logcat.LogPriority
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import mihon.domain.extension.repository.ExtensionStoreRepository
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

private const val VERSION = 67f

internal class TrustExtensionRepositoryMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val sourcePreferences = migrationContext.get<SourcePreferences>()
        val repository = migrationContext.get<ExtensionStoreRepository>()
        if (sourcePreferences == null || repository == null) return false
        withIOContext { migrate(sourcePreferences, repository) }
        return true
    }

    private suspend fun migrate(sourcePreferences: SourcePreferences, repository: ExtensionStoreRepository) {
        for ((index, source) in sourcePreferences.extensionRepos.get().withIndex()) {
            try {
                repository.insertFromPreference(
                    indexUrl = source.removeSuffix("/index.min.json").removeSuffix("/index.json") + "/repo.json",
                    name = "Repo #${index + 1}",
                )
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected) { "Error Migrating Extension Repo with baseUrl: $source" }
            }
        }
        sourcePreferences.extensionRepos.delete()
    }
}
