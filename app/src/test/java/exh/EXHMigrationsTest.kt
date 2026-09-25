package exh

import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.HBROWSE_SOURCE_ID
import exh.source.LEGACY_HBROWSE_SOURCE_ID
import exh.source.LEGACY_NHENTAI_SOURCE_ID
import exh.source.LEGACY_TSUMINO_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

internal class EXHMigrationsTest {
    private fun manga(source: Long, url: String) = Manga.create().copy(source = source, url = url)

    private fun nhentai(url: String) = manga(LEGACY_NHENTAI_SOURCE_ID, url)

    @Test
    fun nhentaiMovesToDelegatedSource() {
        val migrated = EXHMigrations.migrateBackupEntry(nhentai("https://nhentai.net/g/1/?a=b#c"))
        migrated.source shouldBe NHentai.otherId
        migrated.url shouldBe "/g/1/?a=b#c"
    }

    @Test
    fun nhentaiUrlVariants() {
        EXHMigrations.migrateBackupEntry(nhentai("https://nhentai.net/g/1/")).url shouldBe "/g/1/"
        EXHMigrations.migrateBackupEntry(nhentai("https://nhentai.net/g/1/?x")).url shouldBe "/g/1/?x"
        EXHMigrations.migrateBackupEntry(nhentai("https://nhentai.net/g/1/#f")).url shouldBe "/g/1/#f"
        EXHMigrations.migrateBackupEntry(nhentai("http://[bad")).url shouldBe "http://[bad"
    }

    @Test
    fun tsuminoAndHbrowseIdsMigrate() {
        EXHMigrations.migrateBackupEntry(manga(LEGACY_TSUMINO_SOURCE_ID, "/t")).source shouldBe TSUMINO_SOURCE_ID
        val hbrowse = EXHMigrations.migrateBackupEntry(manga(LEGACY_HBROWSE_SOURCE_ID, "/h"))
        hbrowse.source shouldBe HBROWSE_SOURCE_ID
        hbrowse.url shouldBe "/h/c00001/"
    }

    @Test
    fun extensionBackupsMapToEh() {
        val ext = BlacklistedSources.EHENTAI_EXT_SOURCES.first()
        EXHMigrations.migrateBackupEntry(manga(ext, "/g")).source shouldBe EH_SOURCE_ID
    }

    @Test
    fun unrelatedEntriesAreUntouched() {
        val original = manga(1L, "/x")
        EXHMigrations.migrateBackupEntry(original) shouldBeSameInstanceAs original
    }
}
