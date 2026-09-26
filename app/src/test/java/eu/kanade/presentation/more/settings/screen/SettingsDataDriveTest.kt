package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsDataDriveTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val data = DataScreenKoin()
    private val harness = SettingsHarness(compose)
    private var currentDrive: Drive? = null

    @Before
    fun setUp() {
        // The purge row builds its own sync service, which builds its own Drive client.
        mockkConstructor(GoogleDriveService::class)
        every { anyConstructed<GoogleDriveService>().driveService } answers { currentDrive }
        coEvery { anyConstructed<GoogleDriveService>().refreshToken() } just runs
        every { data.googleDrive.getSignInIntent() } returns Intent(Intent.ACTION_VIEW)
        koin.start(data.module())
        koin.sync.syncService.set(SyncManager.SyncService.GOOGLE_DRIVE.value)
        harness.show(SettingsDataScreen)
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun purge(drive: Drive?): String {
        currentDrive = drive
        val shown = ShadowToast.shownToastCount()
        harness.click("Clear Sync Data from Google Drive")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        compose.awaitMain(timeoutMillis = 10_000) { ShadowToast.shownToastCount() > shown }
        return ShadowToast.getTextOfLatestToast().toString()
    }

    private fun drive(found: List<File>, deleteFails: Boolean = false): Drive = mockk(relaxed = true) {
        every { files().list().setSpaces(any()).setQ(any()).setFields(any()).execute().files } returns found.toMutableList()
        if (deleteFails) every { files().delete(any()).execute() } throws IllegalStateException("offline")
    }

    @Test
    fun signInStartsIntent() {
        harness.click("Sign in")
        verify { data.googleDrive.getSignInIntent() }
    }

    @Test
    fun purgeReportsEachOutcome() {
        purge(drive = null) shouldBe "Not signed in to Google Drive"
        purge(drive(found = emptyList())) shouldBe "No sync data found in Google Drive"
        val file = File().setId("sync")
        purge(drive(found = listOf(file))) shouldBe "Sync data purged from Google Drive"
        purge(drive(found = listOf(file), deleteFails = true)) shouldBe
            "Error purging sync data from Google Drive, Try to sign in again."
    }

    @Test
    fun purgeCancelKeepsData() {
        harness.click("Clear Sync Data from Google Drive")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Purge confirmation") shouldBe 0
    }
}
