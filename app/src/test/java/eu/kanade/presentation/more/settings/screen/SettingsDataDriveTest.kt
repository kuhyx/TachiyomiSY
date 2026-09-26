package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.sync.SyncManager
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveSyncService.DeleteSyncDataStatus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkConstructor
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

    @Before
    fun setUp() {
        mockkConstructor(GoogleDriveSyncService::class)
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

    private fun purge(status: DeleteSyncDataStatus): String {
        coEvery { anyConstructed<GoogleDriveSyncService>().deleteSyncDataFromGoogleDrive() } returns status
        val shown = ShadowToast.shownToastCount()
        harness.click("Clear Sync Data from Google Drive")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        compose.awaitMain(timeoutMillis = 10_000) { ShadowToast.shownToastCount() > shown }
        return ShadowToast.getTextOfLatestToast().toString()
    }

    @Test
    fun signInStartsIntent() {
        harness.click("Sign in")
        verify { data.googleDrive.getSignInIntent() }
    }

    @Test
    fun purgeReportsEachOutcome() {
        purge(DeleteSyncDataStatus.NOT_INITIALIZED) shouldBe "Not signed in to Google Drive"
        purge(DeleteSyncDataStatus.NO_FILES) shouldBe "No sync data found in Google Drive"
        purge(DeleteSyncDataStatus.SUCCESS) shouldBe "Sync data purged from Google Drive"
        purge(DeleteSyncDataStatus.ERROR) shouldBe
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
