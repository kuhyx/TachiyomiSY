package exh.debug

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.nHentaiSourceIds
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import mihon.core.migration.MigrationContext
import mihon.core.migration.MigrationJobFactory
import mihon.core.migration.MigrationStrategyFactory
import mihon.core.migration.Migrator
import mihon.core.migration.migrations.migrations
import tachiyomi.data.Database
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Migrations, conversions and one-off repairs. Listed in the debug menu through [DebugFunctions]. */
@Suppress("unused")
internal object DebugMigrationFunctions {
    private val database: Database by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    fun forceUpgradeMigration(): Boolean {
        val migrationContext = MigrationContext(dryrun = false, 0)
        val migrationJobFactory = MigrationJobFactory(migrationContext, Migrator.scope)
        val migrationStrategyFactory = MigrationStrategyFactory(migrationJobFactory, {})
        val strategy = migrationStrategyFactory.create(1, BuildConfig.VERSION_CODE)
        return runBlocking { strategy(migrations).await() }
    }

    fun forceSetupJobs(): Boolean {
        val migrationContext = MigrationContext(dryrun = false, 0)
        val migrationJobFactory = MigrationJobFactory(migrationContext, Migrator.scope)
        val migrationStrategyFactory = MigrationStrategyFactory(migrationJobFactory, {})
        val strategy = migrationStrategyFactory.create(0, BuildConfig.VERSION_CODE)
        return runBlocking { strategy(migrations).await() }
    }

    fun fixReaderViewerBackupBug() {
        runBlocking { database.ehQueries.fixReaderViewerBackupBug() }
    }

    fun resetReaderViewerForAllManga() {
        runBlocking { database.ehQueries.resetReaderViewerForAllManga() }
    }

    fun migrateNhentaiToMultiLang() {
        val sources = nHentaiSourceIds - NHentai.otherId

        runBlocking { database.ehQueries.migrateAllNhentaiToOtherLang(NHentai.otherId, sources) }
    }

    fun exportProtobufScheme() = ProtoBufSchemaGenerator.generateSchemaText(Backup.serializer().descriptor)

    fun convertEhentaiToExhentai() = convertSources(EH_SOURCE_ID, EXH_SOURCE_ID)

    fun convertExhentaiToEhentai() = convertSources(EXH_SOURCE_ID, EH_SOURCE_ID)

    private fun convertSources(from: Long, to: Long) {
        runBlocking {
            database.ehQueries.migrateSource(to, from)
        }
    }
}
