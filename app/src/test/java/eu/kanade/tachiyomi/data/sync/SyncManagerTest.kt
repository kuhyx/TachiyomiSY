package eu.kanade.tachiyomi.data.sync

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.sync.SyncManager.SyncService
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.data.sync.service.manga
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkConstructor
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class SyncManagerTest {

    private val harness = SyncManagerHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun completeText(): String? = shadowOf(harness.context.getSystemService(NotificationManager::class.java))
        .getNotification(Notifications.ID_RESTORE_COMPLETE)
        ?.extras
        ?.getCharSequence(NotificationCompat.EXTRA_TEXT)
        ?.toString()

    @Test
    fun serviceTypesComeFromInts() {
        SyncService.fromInt(0) shouldBe SyncService.NONE
        SyncService.fromInt(1) shouldBe SyncService.SYNCYOMI
        SyncService.fromInt(2) shouldBe SyncService.GOOGLE_DRIVE
        SyncService.fromInt(9) shouldBe SyncService.NONE
        SyncService.entries.map { it.value } shouldBe listOf(0, 1, 2)
    }

    @Test
    fun noServiceSkipsTheRestore() = runTest {
        harness.preferences.syncService.set(0)
        SyncManager(harness.context).syncData()
        harness.logged shouldContain "Invalid sync service type: NONE"
        harness.logged shouldContain "Skip restore due to network issues"
        coVerify { harness.mangas.resetIsSyncing() }
        coVerify { harness.chapters.resetIsSyncing() }
        coVerify { harness.categories.resetIsSyncing() }
    }

    @Test
    fun failedRemoteSkipsTheRestore() = runTest {
        harness.remote = { null }
        SyncManager(harness.context).syncData()
        harness.logged shouldContain "Skip restore due to network issues"
        harness.preferences.lastSyncTimestamp.get() shouldBe 0L
    }

    @Test
    fun overwrittenRemoteIsDone() = runTest {
        SyncManager(harness.context).syncData()
        harness.logged shouldContain "Skip restore due to remote was overwrite from local"
        harness.preferences.lastSyncTimestamp.get() shouldBeGreaterThan 0L
        completeText() shouldBe "Sync completed successfully"
    }

    @Test
    fun googleDriveIsUsedWhenChosen() = runTest {
        mockkConstructor(GoogleDriveSyncService::class)
        coEvery { anyConstructed<GoogleDriveSyncService>().doSync(any()) } returns null
        harness.preferences.syncService.set(SyncService.GOOGLE_DRIVE.value)
        SyncManager(harness.context).syncData()
        coVerify { anyConstructed<GoogleDriveSyncService>().doSync(any()) }
        harness.logged shouldContain "Skip restore due to network issues"
    }

    @Test
    fun emptyRemoteIsAnError() = runTest {
        harness.remote = { Backup(backupManga = emptyList(), backupPreferences = listOf(pref())) }
        SyncManager(harness.context).syncData()
        completeText() shouldBe "No data found on remote server."
        harness.preferences.lastSyncTimestamp.get() shouldBe 0L
    }

    @Test
    fun firstSyncOnlySeedsTheRemote() = runTest {
        harness.preferences.setSyncSettings(SyncSettings(libraryEntries = false))
        every { harness.mangas.getMangasWithFavoriteTimestamp() } returns
            fakeQuery(listOf(favoriteRow(id = 1L, url = "a")))
        harness.remote = { Backup(backupManga = listOf(manga("r"))) }
        SyncManager(harness.context).syncData()
        completeText() shouldBe "Updated remote data successfully"
        harness.preferences.lastSyncTimestamp.get() shouldBeGreaterThan 0L
    }

    @Test
    fun defaultsComeFromInjekt() {
        val manager = SyncManager(harness.context)
        manager.database shouldBe harness.database
        manager.getCategories shouldBe harness.getCategories
        manager.mangaRestorer.isSync shouldBe false
        completeText().shouldBeNull()
    }

    private fun pref() = BackupPreference("k", IntPreferenceValue(1))
}
