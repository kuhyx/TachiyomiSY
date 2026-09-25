package eu.kanade.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class LibraryColumnsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun tearDown() = unmockkAll()

    @Test
    @Config(qualifiers = "land")
    fun landscapeUsesItsOwnColumns() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        harness.libraryPreferences.landscapeColumns.set(4)
        compose.setContent { MaterialTheme { Column { ColumnsSlider(harness.model) } } }
        compose.onNodeWithText("4").assertExists()
    }

    @Test
    fun portraitAutoColumns() {
        val harness = LibrarySettingsHarness(trackerCount = 0)
        compose.setContent { MaterialTheme { Column { ColumnsSlider(harness.model) } } }
        compose.onNodeWithText("Auto").assertExists()
    }

    @Test
    fun releaseHidesIntervalFilter() {
        mockkStatic("eu.kanade.tachiyomi.util.system.BuildConfigKt")
        every { isDebugBuildType } returns false
        val harness = LibrarySettingsHarness(trackerCount = 0)
        compose.setContent { MaterialTheme { Column { FilterPage(harness.model) } } }
        compose.onNodeWithText("Customized update frequency").assertDoesNotExist()
    }
}
