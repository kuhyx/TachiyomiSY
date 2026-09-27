package mihon.feature.migration.config

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Drags the handle of a selected source; the list reorders only among the selected sources. */
@RunWith(RobolectricTestRunner::class)
internal class MigrationConfigDragTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MigrationConfigHarness()

    @Before
    fun setUp() {
        harness.start()
        harness.sources = {
            listOf(httpSource(1L, name = "Alpha"), httpSource(2L, name = "Beta"), httpSource(3L, name = "Gamma"))
        }
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        harness.stop()
    }

    private fun show() {
        compose.setContent { ScreenHost(MigrationConfigScreen(listOf(1L, 2L))) }
        compose.waitForLabel("Continue")
    }

    // Presses the trailing drag handle of [label]'s row and drags it by [rows] row heights.
    private fun drag(label: String, rows: Float) {
        compose.onNodeWithText(label).performTouchInput {
            val start = Offset(width - 28.dp.toPx(), centerY)
            down(start)
            val steps = 20
            repeat(steps) {
                advanceEventTime(16L)
                moveBy(Offset(0f, height * rows / steps))
            }
            advanceEventTime(16L)
            up()
        }
        compose.waitForIdle()
    }

    @Test
    fun draggingReordersSelected() {
        harness.preferences.migrationSources.set(listOf(1L, 2L, 3L))
        show()
        drag("Alpha", rows = 2.2f)
        harness.preferences.migrationSources.get() shouldContainExactly listOf(2L, 3L, 1L)
    }

    @Test
    fun draggingIntoAvailableIsIgnored() {
        harness.preferences.migrationSources.set(listOf(1L, 2L))
        show()
        drag("Beta", rows = 3.5f)
        harness.preferences.migrationSources.get() shouldContainExactly listOf(1L, 2L)
    }
}
