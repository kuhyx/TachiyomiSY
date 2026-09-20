package mihon.core.migration.migrations

import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.LEGACY_NHENTAI_SOURCE_ID
import mihon.core.migration.MigrateUtils
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext

private const val VERSION = 6f

internal class DelegateNHentaiMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean = withIOContext {
        MigrateUtils.updateSourceId(migrationContext, NHentai.otherId, LEGACY_NHENTAI_SOURCE_ID)

        true
    }
}
