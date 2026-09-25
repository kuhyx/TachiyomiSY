package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.backupFlatMetadata
import exh.metadata.metadata.base.FlatMetadata
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.GetAllMangaSourceAndUrl
import tachiyomi.domain.manga.model.CustomMangaInfo

internal class MangaRestorerTest {

    private val harness = MangaRestorerHarness()

    @BeforeEach
    fun setUp() = harness.start()

    @AfterEach
    fun tearDown() = harness.stop()

    private fun verifyPersisted(version: Long, favorite: Boolean, initialized: Boolean) = coVerify {
        harness.graph.mangas.update(
            source = any(),
            url = any(),
            artist = any(),
            author = any(),
            description = any(),
            genre = any(),
            title = any(),
            status = any(),
            thumbnailUrl = any(),
            favorite = favorite,
            lastUpdate = any(),
            nextUpdate = null,
            initialized = initialized,
            viewer = any(),
            chapterFlags = any(),
            coverLastModified = any(),
            dateAdded = any(),
            updateStrategy = any(),
            calculateInterval = null,
            version = version,
            isSyncing = 1L,
            notes = any(),
            memo = any(),
            mangaId = 1L,
        )
    }

    @Test
    fun newEntriesSortFirst() = runTest {
        every { harness.graph.mangas.getAllMangaSourceAndUrl() } returns
            fakeQuery(listOf(GetAllMangaSourceAndUrl(source = 1L, url = "/old")))
        val old = BackupManga(source = 1L, url = "/old", lastModifiedAt = 9)
        val newA = BackupManga(source = 1L, url = "/a", lastModifiedAt = 1)
        val newB = BackupManga(source = 2L, url = "/b", lastModifiedAt = 5)
        harness.restorer().sortByNew(listOf(old, newA, newB)) shouldBe listOf(newB, newA, old)
    }

    @Test
    fun newEntriesAreInserted() = runTest {
        harness.restorer().restore(BackupManga(source = 1L, url = "/m"), emptyList())
        coVerify {
            harness.graph.updateManga.awaitUpdateFetchInterval(
                match { it.id == MangaRestorerHarness.NEW_ID },
                any(),
                any(),
            )
        }
        coVerify(exactly = 0) { harness.graph.mangas.resetIsSyncing() }
    }

    @Test
    fun newerBackupUpdatesTheEntry() = runTest {
        coEvery { harness.graph.getMangaByUrlAndSourceId.await("/m", 1L) } returns dbManga(version = 1)
        val backup = BackupManga(source = 1L, url = "/m", version = 2, favorite = false, initialized = true)
        harness.restorer(isSync = true).restore(backup, emptyList())
        verifyPersisted(version = 2, favorite = true, initialized = true)
        coVerify { harness.graph.mangas.resetIsSyncing() }
        coVerify { harness.graph.chapters.resetIsSyncing() }
    }

    @Test
    fun olderBackupKeepsTheEntry() = runTest {
        coEvery { harness.graph.getMangaByUrlAndSourceId.await("/m", 1L) } returns
            dbManga(version = 3, favorite = false)
        harness.restorer().restore(BackupManga(source = 1L, url = "/m", version = 1, favorite = false), emptyList())
        verifyPersisted(version = 3, favorite = false, initialized = false)
    }

    @Test
    fun metadataAndEditsAreRestored() = runTest {
        val backup = BackupManga(source = 1L, url = "/m", flatMetadata = backupFlatMetadata(), customTitle = "Mine")
        harness.restorer().restore(backup, emptyList())
        coVerify { harness.graph.insertFlatMetadata.await(any<FlatMetadata>()) }
        val edits = CustomMangaInfo(id = MangaRestorerHarness.NEW_ID, title = "Mine")
        verify { harness.graph.setCustomMangaInfo.set(edits) }
    }

    @Test
    fun copyKeepsEitherFlag() {
        with(harness.restorer()) {
            val stored = dbManga(favorite = true).copy(initialized = true)
            val fresh = dbManga(favorite = false).copy(initialized = false)
            stored.copyFrom(fresh).favorite shouldBe true
            stored.copyFrom(fresh).initialized shouldBe true
            fresh.copyFrom(stored).favorite shouldBe true
            fresh.copyFrom(stored).initialized shouldBe true
            fresh.copyFrom(fresh).favorite shouldBe false
        }
    }

    @Test
    fun customInfoNeedsAField() {
        with(harness.restorer()) {
            BackupManga(source = 1L, url = "/m").getCustomMangaInfo().shouldBeNull()
            BackupManga(source = 1L, url = "/m", customTitle = "T").getCustomMangaInfo() shouldBe
                CustomMangaInfo(id = 0L, title = "T")
            BackupManga(source = 1L, url = "/m", customStatus = 2).getCustomMangaInfo()?.status shouldBe 2L
        }
    }
}
