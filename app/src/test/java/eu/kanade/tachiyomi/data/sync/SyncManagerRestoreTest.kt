package eu.kanade.tachiyomi.data.sync

import android.net.Uri
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.data.sync.service.category
import eu.kanade.tachiyomi.data.sync.service.manga
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class SyncManagerRestoreTest {

    private val harness = SyncManagerHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.preferences.lastSyncTimestamp.set(1L)
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun unchangedRemoteIsDone() = runTest {
        every { harness.mangas.getAllManga() } returns fakeQuery(listOf(mangasRow(id = 1L, url = "r")))
        harness.remote = { it.backup?.copy(backupManga = listOf(manga("r"))) }
        SyncManager(harness.context).syncData()
        harness.logged shouldContain "Already up-to-date favorite: r"
        verify(exactly = 0) { BackupRestoreJob.start(any(), any(), any(), any()) }
        harness.preferences.lastSyncTimestamp.get() shouldBeGreaterThan 1L
    }

    @Test
    fun changesAreRestored() = runTest {
        every { harness.mangas.getAllManga() } returns fakeQuery(listOf(mangasRow(id = 2L, url = "nonfav")))
        coEvery { harness.getCategories.await() } returns listOf(
            Category(id = 0L, name = "", order = 0L, flags = 0L),
            Category(id = 5L, name = "Old", order = 1L, flags = 0L, uid = 9L),
            Category(id = 6L, name = "Kept", order = 2L, flags = 0L, uid = 3L),
            Category(id = 7L, name = "Named", order = 3L, flags = 0L, uid = 4L),
        )
        harness.remote = {
            it.backup?.copy(
                backupManga = listOf(manga("new"), manga("nonfav", favorite = false)),
                backupCategories = listOf(category("Other", uid = 3L), category("Named", uid = 99L)),
            )
        }
        val uri = slot<Uri>()
        every { BackupRestoreJob.start(any(), capture(uri), any(), any()) } returns Unit
        SyncManager(harness.context).syncData()
        harness.verifyFavoriteWritten(mangaId = 2L, favorite = false)
        coVerify(exactly = 1) { harness.categories.delete(any()) }
        coVerify { harness.categories.delete(5L) }
        verify { BackupRestoreJob.start(harness.context, any(), RestoreOptions(), sync = true) }
        File(checkNotNull(uri.captured.path)).exists() shouldBe true
        harness.preferences.lastSyncTimestamp.get() shouldBeGreaterThan 1L
        harness.logged shouldContain "Adding to favorites: new"
        harness.logged shouldContain "Adding to non-favorites: nonfav"
    }

    @Test
    fun nothingToDeleteNeedsNoWrite() = runTest {
        coEvery { harness.getCategories.await() } returns listOf(Category(id = 0L, name = "", order = 0L, flags = 0L))
        harness.remote = { it.backup?.copy(backupManga = listOf(manga("new"))) }
        SyncManager(harness.context).syncData()
        coVerify(exactly = 0) { harness.categories.delete(any()) }
        verify { BackupRestoreJob.start(harness.context, any(), RestoreOptions(), sync = true) }
    }

    @Test
    fun categoriesOffKeepsThemAll() = runTest {
        harness.preferences.setSyncSettings(SyncSettings(categories = false))
        val old = Category(id = 5L, name = "Old", order = 1L, flags = 0L)
        coEvery { harness.getCategories.await() } returns listOf(old)
        harness.remote = { it.backup?.copy(backupManga = listOf(manga("new"))) }
        SyncManager(harness.context).syncData()
        coVerify(exactly = 0) { harness.categories.delete(any()) }
        verify { BackupRestoreJob.start(harness.context, any(), RestoreOptions(categories = false), sync = true) }
    }

    @Test
    fun emptyFirstSyncRestores() = runTest {
        harness.preferences.lastSyncTimestamp.set(0L)
        harness.remote = { it.backup?.copy(backupManga = listOf(manga("new"))) }
        SyncManager(harness.context).syncData()
        verify { BackupRestoreJob.start(harness.context, any(), RestoreOptions(), sync = true) }
    }

    @Test
    fun cacheFailureStopsTheRestore() = runTest {
        File(harness.context.cacheDir, "tachiyomi_sync_data.proto.gz").mkdirs()
        harness.remote = { it.backup?.copy(backupManga = listOf(manga("new"))) }
        SyncManager(harness.context).syncData()
        harness.logged shouldContain "Failed to write sync data to file"
        verify(exactly = 0) { BackupRestoreJob.start(any(), any(), any(), any()) }
        harness.preferences.lastSyncTimestamp.get() shouldBe 1L
    }
}
