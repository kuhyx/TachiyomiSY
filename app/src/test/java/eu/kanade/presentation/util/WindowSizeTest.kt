package eu.kanade.presentation.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class WindowSizeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun phonesAreNotTablets() {
        compose.setContent { Text("tablet ${isTabletUi()}") }
        compose.onNodeWithText("tablet false").assertExists()
    }

    @Test
    @Config(qualifiers = "sw720dp")
    fun wideScreensAreTablets() {
        compose.setContent { Text("tablet ${isTabletUi()}") }
        compose.onNodeWithText("tablet true").assertExists()
    }

    @Test
    fun fastScrollItemsAnimate() {
        compose.setContent {
            LazyColumn {
                item { Box(Modifier.animateItemFastScroll()) { Text("row") } }
            }
        }
        compose.onNodeWithText("row").assertExists()
    }
}
