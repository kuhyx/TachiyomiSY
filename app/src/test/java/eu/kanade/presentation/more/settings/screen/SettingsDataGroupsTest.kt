package eu.kanade.presentation.more.settings.screen

import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.util.system.DeviceUtil
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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsDataGroupsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val data = DataScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        mockkObject(BackupRestoreJob.Companion, BackupCreateJob.Companion, DeviceUtil)
        every { BackupRestoreJob.isRunning(any()) } returns false
        every { BackupCreateJob.setupTask(any(), any()) } just runs
        every { DeviceUtil.isMiui } returns false
        koin.start(data.module())
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun toast(): String = ShadowToast.getTextOfLatestToast().toString()

    @Test
    fun createOpensScreen() {
        harness.show(SettingsDataScreen)
        tap("Create backup")
        verify { harness.navigator.push(any<CreateBackupScreen>()) }
    }

    @Test
    fun restorePicksFile() {
        harness.registry.answer = { Uri.parse("content://backup/file") }
        harness.show(SettingsDataScreen)
        tap("Restore backup")
        verify { harness.navigator.push(any<RestoreBackupScreen>()) }
    }

    @Test
    fun restoreWithoutFileToasts() {
        every { DeviceUtil.isMiui } returns true
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns false
        harness.show(SettingsDataScreen)
        tap("Restore backup")
        toast() shouldBe "No file selected"
    }

    @Test
    fun restoreWarnsOnMiui() {
        every { DeviceUtil.isMiui } returns true
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns true
        harness.show(SettingsDataScreen)
        harness.registry.failure = IllegalStateException("stop after the warning")
        runCatching { tap("Restore backup") }
        ShadowToast.shownToastCount() shouldBe 1
    }

    @Test
    fun restoreBusyToasts() {
        every { BackupRestoreJob.isRunning(any()) } returns true
        harness.show(SettingsDataScreen)
        tap("Restore backup")
        toast() shouldBe "Restore is already in progress"
        harness.registry.launched shouldBe emptyList()
    }

    @Test
    fun intervalSchedulesBackup() {
        harness.show(SettingsDataScreen)
        harness.list("Automatic backup frequency", 6) shouldBe true
        verify { BackupCreateJob.setupTask(any(), 6) }
    }

    @Test
    fun cacheClearedToasts() {
        every { data.chapterCache.clear() } returns 4
        harness.show(SettingsDataScreen)
        harness.click("Clear chapter cache")
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        toast() shouldBe "Cache cleared, 4 files deleted"
    }

    @Test
    fun cacheErrorToasts() {
        every { data.previewCache.clear() } throws IllegalStateException("disk")
        harness.show(SettingsDataScreen)
        harness.click("Clear page preview cache")
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        toast() shouldBe "Error occurred while clearing"
    }
}
