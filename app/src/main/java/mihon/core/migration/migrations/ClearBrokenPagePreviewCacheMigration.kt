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

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val pagePreviewCache = migrationContext.get<PagePreviewCache>()
        if (context == null || pagePreviewCache == null) return false
        withIOContext { migrate(context, pagePreviewCache) }
        return true
    }

    private fun migrate(context: Application, pagePreviewCache: PagePreviewCache) {
        pagePreviewCache.clear()
        File(context.cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).listFiles()
            ?.filterNot { it.name == "journal" || it.name.startsWith("journal.") }
            ?.forEach {
                // delete() reports failure by returning false rather than throwing.
                if (!it.delete()) logcat(LogPriority.WARN) { "Failed to remove ${it.name} from the cache" }
            }
    }
}
