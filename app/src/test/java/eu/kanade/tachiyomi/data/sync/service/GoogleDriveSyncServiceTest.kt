package eu.kanade.tachiyomi.data.sync.service

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.api.services.drive.Drive
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class GoogleDriveSyncServiceTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val preferences = SyncPreferences(FlowPreferenceStore())
    private val fake = FakeDrive()
    private var drives: List<Drive?> = listOf(fake.drive)
    private var logged = mutableListOf<String>()

    @Before
    fun setUp() {
        logged = captureLogcat()
        startKoin { modules(module { single { preferences } }, module { single<ProtoBuf> { ProtoBuf } }) }
        mockkConstructor(GoogleDriveService::class)
        coEvery { anyConstructed<GoogleDriveService>().refreshToken() } just runs
        every { anyConstructed<GoogleDriveService>().driveService } returnsMany drives
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    private fun service(): GoogleDriveSyncService {
        every { anyConstructed<GoogleDriveService>().driveService } returnsMany drives
        return GoogleDriveSyncService(app, Json, preferences)
    }

    @Test
    fun notSignedInFails() = runTest {
        drives = listOf(null)
        service().doSync(syncData("a")).shouldBeNull()
        logged shouldContain "Error syncing: Not signed in to Google Drive"
    }

    @Test
    fun firstSyncCreatesTheFile() = runTest {
        val data = syncData("a")
        service().doSync(data) shouldBeSameInstanceAs data.backup
        val (metadata, uploaded) = fake.uploads.single()
        metadata.name shouldBe "TachiyomiSY_sync.proto.gz"
        metadata.mimeType shouldBe "application/octet-stream"
        metadata.parents shouldBe listOf("appDataFolder")
        metadata.appProperties shouldBe mapOf("deviceId" to "device")
        uploaded.backupManga.single().url shouldBe "a"
        logged shouldContain "No files found in app data"
    }

    @Test
    fun sameDeviceOverwrites() = runTest {
        fake.listing("f1" to preferences.uniqueDeviceID())
        fake.download(Backup(backupManga = listOf(manga("remote"))))
        val data = syncData("local")
        service().doSync(data) shouldBeSameInstanceAs data.backup
        verify { fake.files.update("f1", any(), any()) }
        fake.uploads.single().second.backupManga.map { it.url } shouldBe listOf("local")
    }

    @Test
    fun otherDeviceIsMerged() = runTest {
        fake.listing("f1" to "other")
        fake.download(Backup(backupManga = listOf(manga("remote"))))
        service().doSync(syncData("local"))?.backupManga?.map { it.url } shouldBe listOf("local", "remote")
        fake.uploads.single().first.appProperties shouldBe mapOf("deviceId" to preferences.uniqueDeviceID())
    }

    @Test
    fun unknownDeviceIsMerged() = runTest {
        fake.listing("f1" to null)
        fake.download(Backup(backupManga = listOf(manga("remote"))))
        service().doSync(syncData("local"))?.backupManga?.size shouldBe 2
        logged shouldContain "Local device ID: ${preferences.uniqueDeviceID()}, Last sync device ID: "
    }

    @Test
    fun failedDownloadAborts() = runTest {
        fake.listing("f1" to "other")
        every { fake.get.executeMediaAsInputStream() } throws IOException("offline")
        service().doSync(syncData("a")).shouldBeNull()
        logged shouldContain "Error syncing: Failed to download sync data: offline"
        fake.uploads shouldBe emptyList()
    }

    @Test
    fun failedListingCountsAsEmpty() = runTest {
        every { fake.list.execute() } throws IOException("quota")
        service().doSync(syncData("a"))?.backupManga?.size shouldBe 1
        verify { fake.files.create(any(), any()) }
        logged.any { it.startsWith("Error no sync data found in appData folder") } shouldBe true
    }

    @Test
    fun nothingToPushIsANoOp() = runTest {
        service().doSync(SyncData()).shouldBeNull()
        fake.uploads shouldBe emptyList()
    }

    @Test
    fun emptyBackupIsRefused() = runTest {
        service().doSync(SyncData(backup = Backup(backupManga = emptyList()))).shouldBeNull()
        logged shouldContain "Error syncing: No library entries to back up"
    }

    @Test
    fun signOutMidSyncFails() = runTest {
        drives = listOf(fake.drive, null)
        service().doSync(syncData("a")).shouldBeNull()
        fake.uploads shouldBe emptyList()
    }
}
