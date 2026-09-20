package mihon.core.migration.migrations

import android.app.Application
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import logcat.LogPriority
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import java.io.File

private const val VERSION = 58f

internal class ClearBrokenPagePreviewCacheMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val context = migrationContext.get<Application>() ?: return@withIOContext false
        val pagePreviewCache = migrationContext.get<PagePreviewCache>() ?: return@withIOContext false
        pagePreviewCache.clear()
        File(context.cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).listFiles()?.forEach {
            if (it.name == "journal" || it.name.startsWith("journal.")) {
                return@forEach
            }

            try {
                it.delete()
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.WARN, expected) { "Failed to remove file from cache" }
            }
        }

        return@withIOContext true
    }
}
