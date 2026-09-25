package exh.debug

import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.nHentaiSourceIds
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.shouldContain
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.domain.source.service.SourceManager

internal class DebugMigrationFunctionsTest {
    @Test
    fun readerViewerRepairs() {
        DebugMigrationFunctions.fixReaderViewerBackupBug()
        DebugMigrationFunctions.resetReaderViewerForAllManga()
        coVerify(exactly = 1) { queries.fixReaderViewerBackupBug() }
        coVerify(exactly = 1) { queries.resetReaderViewerForAllManga() }
    }

    @Test
    fun conversionsGoBothWays() {
        DebugMigrationFunctions.convertEhentaiToExhentai()
        DebugMigrationFunctions.convertExhentaiToEhentai()
        coVerify(exactly = 1) { queries.migrateSource(EXH_SOURCE_ID, EH_SOURCE_ID) }
        coVerify(exactly = 1) { queries.migrateSource(EH_SOURCE_ID, EXH_SOURCE_ID) }
    }

    @Test
    fun nhentaiSkipsOtherLanguage() {
        val saved = nHentaiSourceIds
        nHentaiSourceIds = listOf(1L, NHentai.otherId)
        DebugMigrationFunctions.migrateNhentaiToMultiLang()
        coVerify(exactly = 1) { queries.migrateAllNhentaiToOtherLang(NHentai.otherId, listOf(1L)) }
        nHentaiSourceIds = saved
    }

    @Test
    fun protobufSchemeHasBackup() {
        val scheme = DebugMigrationFunctions.exportProtobufScheme()
        scheme shouldContain "syntax = \"proto2\";"
        scheme shouldContain "Backup"
    }

    /** No collaborator is registered, so the migrations that need one bail out. */
    @Test
    fun migrationsRunWithoutDeps() {
        DebugMigrationFunctions.forceUpgradeMigration().shouldBeTrue()
        DebugMigrationFunctions.forceSetupJobs().shouldBeTrue()
    }

    private companion object {
        val queries = mockk<EhQueries>(relaxed = true)
        val database = mockk<Database> { every { ehQueries } returns queries }
        val sourceManager = mockk<SourceManager>()

        @JvmStatic
        @BeforeAll
        fun before() {
            stopKoin()
            startKoin {
                modules(
                    module {
                        single { database }
                        single<SourceManager> { sourceManager }
                    },
                )
            }
        }

        @JvmStatic
        @AfterAll
        fun after() = stopKoin()
    }
}
