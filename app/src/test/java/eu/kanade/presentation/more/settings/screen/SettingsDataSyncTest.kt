package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import eu.kanade.presentation.more.settings.screen.data.SyncSettingsSelector
import eu.kanade.presentation.more.settings.screen.data.SyncTriggerOptionsScreen
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.sync.SyncManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SettingsDataSyncTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val data = DataScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkObject(SyncDataJob.Companion)
        every { SyncDataJob.isRunning(any()) } returns false
        every { SyncDataJob.startNow(any(), any()) } just runs
        every { SyncDataJob.setupTask(any(), any()) } just runs
        koin.start(data.module())
        koin.sync.syncService.set(SyncManager.SyncService.SYNCYOMI.value)
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun scan(contents: String?): (ActivityResultContract<*, *>) -> Any? = { contract ->
        val intent = Intent().apply { contents?.let { putExtra(Intents.Scan.RESULT, it) } }
        (contract as ScanContract).parseResult(Activity.RESULT_OK, intent)
    }

    @Test
    fun noServiceHidesRest() {
        koin.sync.syncService.set(SyncManager.SyncService.NONE.value)
        harness.show(SettingsDataScreen)
        harness.list("Service", SyncManager.SyncService.GOOGLE_DRIVE.value) shouldBe true
        harness.items().none { it.title == "Sync now" } shouldBe true
    }

    @Test
    fun hostIsTrimmed() {
        harness.show(SettingsDataScreen)
        harness.edit("Host", " https://sync.example/// ") shouldBe true
        koin.sync.clientHost.get() shouldBe "https://sync.example"
    }

    @Test
    fun apiKeyTypedAndScanned() {
        harness.show(SettingsDataScreen)
        compose.onNodeWithText("API key").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("typed")
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        koin.sync.clientAPIKey.get() shouldBe "typed"
        harness.registry.answer = scan(contents = null)
        compose.onNodeWithContentDescription("Scan a QR code").performClick()
        harness.registry.answer = scan(contents = "")
        compose.onNodeWithContentDescription("Scan a QR code").performClick()
        koin.sync.clientAPIKey.get() shouldBe "typed"
        harness.registry.answer = scan(contents = "scanned")
        compose.onNodeWithContentDescription("Scan a QR code").performClick()
        koin.sync.clientAPIKey.get() shouldBe "scanned"
    }

    @Test
    fun navigationRows() {
        harness.show(SettingsDataScreen)
        harness.click("Choose what to sync")
        verify { harness.navigator.push(any<SyncSettingsSelector>()) }
        harness.click("Create sync triggers")
        verify { harness.navigator.push(any<SyncTriggerOptionsScreen>()) }
    }

    @Test
    fun syncNowStartsOrToasts() {
        harness.show(SettingsDataScreen)
        harness.click("Sync now")
        verify { SyncDataJob.startNow(any(), manual = true) }
        every { SyncDataJob.isRunning(any()) } returns true
        harness.click("Sync now")
        ShadowToast.getTextOfLatestToast().toString() shouldBe "Sync is already in progress"
    }

    @Test
    fun intervalSchedulesSync() {
        harness.show(SettingsDataScreen)
        harness.list("Synchronization frequency", 30) shouldBe true
        verify { SyncDataJob.setupTask(any(), 30) }
    }
}
