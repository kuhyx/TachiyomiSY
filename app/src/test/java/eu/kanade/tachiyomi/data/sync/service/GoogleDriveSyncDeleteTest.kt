package eu.kanade.tachiyomi.data.sync.service

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.api.services.drive.Drive
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService.DeleteSyncDataStatus
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
internal class GoogleDriveSyncDeleteTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val preferences = SyncPreferences(FlowPreferenceStore())
    private val fake = FakeDrive()
    private var logged = mutableListOf<String>()

    @Before
    fun setUp() {
        logged = captureLogcat()
        startKoin { modules(module { single { preferences } }, module { single<ProtoBuf> { ProtoBuf } }) }
        mockkConstructor(GoogleDriveService::class)
        coEvery { anyConstructed<GoogleDriveService>().refreshToken() } just runs
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    private fun serviceFromInjekt(drive: Drive?): GoogleDriveSyncService {
        every { anyConstructed<GoogleDriveService>().driveService } returns drive
        return GoogleDriveSyncService(app)
    }

    @Test
    fun signedOutIsNotInitialized() = runTest {
        serviceFromInjekt(drive = null).deleteSyncDataFromGoogleDrive() shouldBe DeleteSyncDataStatus.NOT_INITIALIZED
        logged shouldContain "Google Drive service not initialized"
        coVerify(exactly = 0) { anyConstructed<GoogleDriveService>().refreshToken() }
    }

    @Test
    fun noFilesIsReported() = runTest {
        serviceFromInjekt(fake.drive).deleteSyncDataFromGoogleDrive() shouldBe DeleteSyncDataStatus.NO_FILES
        logged shouldContain "No sync data file found in appData folder of Google Drive"
    }

    @Test
    fun everyFileIsDeleted() = runTest {
        fake.listing("f1" to "a", "f2" to "b")
        serviceFromInjekt(fake.drive).deleteSyncDataFromGoogleDrive() shouldBe DeleteSyncDataStatus.SUCCESS
        verify { fake.files.delete("f1") }
        verify { fake.files.delete("f2") }
        logged shouldContain "Deleted sync data file in appData folder of Google Drive with file ID: f2"
    }

    @Test
    fun failedDeleteIsAnError() = runTest {
        fake.listing("f1" to "a")
        every { fake.delete.execute() } throws IOException("denied")
        serviceFromInjekt(fake.drive).deleteSyncDataFromGoogleDrive() shouldBe DeleteSyncDataStatus.ERROR
        logged.any { it.startsWith("Error occurred while interacting with Google Drive") } shouldBe true
    }

    @Test
    fun statusValues() {
        DeleteSyncDataStatus.entries.map { it.name } shouldBe listOf("NOT_INITIALIZED", "NO_FILES", "SUCCESS", "ERROR")
    }
}
