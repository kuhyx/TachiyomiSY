package mihon.core.migration.migrations

import eu.kanade.domain.manga.interactor.UpdateManga
import exh.source.HBROWSE_SOURCE_ID
import exh.source.LEGACY_HBROWSE_SOURCE_ID
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.interactor.GetMangaBySource
import tachiyomi.domain.manga.model.MangaUpdate

private const val VERSION = 4f

internal class DelegateHBrowseMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        val getMangaBySource = migrationContext.get<GetMangaBySource>() ?: return@withIOContext false
        val updateManga = migrationContext.get<UpdateManga>() ?: return@withIOContext false
        MigrateUtils.updateSourceId(migrationContext, HBROWSE_SOURCE_ID, LEGACY_HBROWSE_SOURCE_ID)

        // Migrate BHrowse URLs
        val hBrowseManga = getMangaBySource.await(HBROWSE_SOURCE_ID)
        val mangaUpdates = hBrowseManga.map {
            MangaUpdate(it.id, url = it.url + "/c00001/")
        }
        updateManga.awaitAll(mangaUpdates)
        return@withIOContext true
    }
}
