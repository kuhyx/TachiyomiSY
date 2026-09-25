package eu.kanade.presentation.more.settings.screen.data

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class CreateBackupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val registry = FakeResultRegistry()
    private val options = slot<BackupOptions>()

    @Before
    fun setUp() {
        mockkObject(BackupCreateJob.Companion, DeviceUtil)
        every { BackupCreateJob.isManualJobRunning(any()) } returns false
        every { BackupCreateJob.startNow(any(), any(), capture(options)) } just runs
        every { DeviceUtil.isMiui } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun create() {
        compose.onNodeWithText("Create").performClick()
        compose.waitForIdle()
    }

    @Test
    fun createsChosenBackup() {
        registry.answer = { Uri.parse("content://backups/new.tachibk") }
        compose.showAbove(CreateBackupScreen(), registry)
        compose.onNodeWithText("Categories").performClick()
        create()
        options.captured.categories shouldBe false
        count("Below") shouldBe 1
    }

    @Test
    fun cancelledPickerStays() {
        compose.showAbove(CreateBackupScreen(), registry)
        create()
        options.isCaptured shouldBe false
        count("Below") shouldBe 0
    }

    @Test
    fun runningBackupToasts() {
        every { BackupCreateJob.isManualJobRunning(any()) } returns true
        compose.showAbove(CreateBackupScreen(), registry)
        create()
        ShadowToast.getTextOfLatestToast().toString() shouldBe "Backup is already in progress"
    }

    @Test
    fun missingPickerToasts() {
        registry.failure = ActivityNotFoundException()
        compose.showAbove(CreateBackupScreen(), registry)
        create()
        ShadowToast.getTextOfLatestToast().toString() shouldBe "No file picker app found"
    }

    @Test
    fun miuiWarning() {
        every { DeviceUtil.isMiui } returns true
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns true
        compose.showAbove(CreateBackupScreen(), registry)
        count("Backup/restore may not function properly if MIUI Optimization is disabled.") shouldBe 1
    }

    @Test
    fun miuiOptimizedNoWarning() {
        every { DeviceUtil.isMiui } returns true
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns false
        compose.showAbove(CreateBackupScreen(), registry)
        count("Backup/restore may not function properly if MIUI Optimization is disabled.") shouldBe 0
    }
}
