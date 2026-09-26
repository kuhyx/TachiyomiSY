package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.source.service.SourceManager

private const val URI = "content://backups/old.tachibk"

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class RestoreBackupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<SourceManager>() }
                    single { mockk<TrackerManager>() }
                },
            )
        }
        mockkConstructor(BackupFileValidator::class)
        mockkObject(BackupRestoreJob.Companion, DeviceUtil)
        every { BackupRestoreJob.start(any(), any(), any()) } just runs
        every { DeviceUtil.isMiui } returns true
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun valid(sources: List<String>, trackers: List<String>) {
        every { anyConstructed<BackupFileValidator>().validate(any()) } returns
            BackupFileValidator.Results(missingSources = sources, missingTrackers = trackers)
    }

    private fun count(text: String, substring: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().size

    @Test
    fun validBackupRestores() {
        valid(sources = emptyList(), trackers = emptyList())
        compose.showAbove(RestoreBackupScreen(URI))
        count("Backup/restore may not function properly if MIUI Optimization is disabled.") shouldBe 1
        compose.onNodeWithText("Categories").performClick()
        compose.onNodeWithText("Restore").performClick()
        compose.waitForIdle()
        verify { BackupRestoreJob.start(any(), any(), any()) }
        count("Below") shouldBe 1
    }

    @Test
    fun nothingChosenCannotRestore() {
        every { DeviceUtil.isMiuiOptimizationDisabled() } returns false
        valid(sources = emptyList(), trackers = emptyList())
        compose.showAbove(RestoreBackupScreen(URI))
        count("Backup/restore may not function properly if MIUI Optimization is disabled.") shouldBe 0
        listOf("Library", "Categories", "App settings", "Extension stores", "Source settings", "Saved Searches")
            .forEach { compose.onNodeWithText(it).performClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Restore").assertIsNotEnabled()
    }

    @Test
    fun missingSourcesListed() {
        valid(sources = listOf("Source A"), trackers = emptyList())
        compose.showAbove(RestoreBackupScreen(URI))
        count("Missing sources:", substring = true) shouldBe 1
        count("Trackers not logged into:", substring = true) shouldBe 0
    }

    @Test
    fun missingTrackersListed() {
        valid(sources = emptyList(), trackers = listOf("AniList", "Kitsu"))
        compose.showAbove(RestoreBackupScreen(URI))
        count("- AniList\n- Kitsu", substring = true) shouldBe 1
    }

    @Test
    fun invalidFileShowsError() {
        every { DeviceUtil.isMiui } returns false
        every { anyConstructed<BackupFileValidator>().validate(any()) } throws IllegalStateException("corrupt")
        compose.showAbove(RestoreBackupScreen(URI))
        count("Invalid backup file:", substring = true) shouldBe 1
        count("corrupt", substring = true) shouldBe 1
    }

    @Test
    fun otherErrorsPrinted() {
        var text = ""
        compose.setContent { MaterialTheme { text = restoreErrorMessage("plain").text } }
        compose.waitForIdle()
        text shouldContain "plain"
    }
}
