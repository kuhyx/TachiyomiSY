package eu.kanade.presentation.more.settings.screen

import android.net.Uri
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.export.LibraryExporter
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class SettingsDataExportTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val data = DataScreenKoin()
    private val harness = SettingsHarness(compose)
    private var options: ExportOptions? = null

    @Before
    fun setUp() {
        mockkObject(LibraryExporter)
        coEvery {
            LibraryExporter.exportToCsv(
                context = any(),
                uri = any(),
                favorites = any(),
                options = any(),
                onExportComplete = any(),
            )
        } coAnswers {
            options = arg(3)
            arg<() -> Unit>(4).invoke()
        }
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

    @Test
    fun exportWithChosenColumns() {
        harness.registry.answer = { Uri.parse("content://export/library.csv") }
        harness.show(SettingsDataScreen)
        harness.click("Library List")
        val boxes = compose.onAllNodes(isToggleable())
        boxes[1].performClick()
        boxes[0].performClick()
        compose.waitForIdle()
        boxes[0].performClick()
        boxes[2].performClick()
        tap("Save")
        compose.waitUntil(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        options shouldBe ExportOptions(includeTitle = true, includeAuthor = false, includeArtist = true)
        ShadowToast.getTextOfLatestToast().toString() shouldBe "Library Exported"
    }

    @Test
    fun cancelledSaveExportsNothing() {
        harness.show(SettingsDataScreen)
        harness.click("Library List")
        tap("Save")
        coVerify(exactly = 0) {
            LibraryExporter.exportToCsv(
                context = any(),
                uri = any(),
                favorites = any(),
                options = any(),
                onExportComplete = any(),
            )
        }
    }

    @Test
    fun dialogCancelCloses() {
        harness.show(SettingsDataScreen)
        harness.click("Library List")
        tap("Cancel")
        compose.onAllNodesWithText("Select data to include").fetchSemanticsNodes().size shouldBe 0
    }
}
