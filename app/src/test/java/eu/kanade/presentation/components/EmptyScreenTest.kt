package eu.kanade.presentation.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmptyScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun noActionPreviewRenders() {
        compose.setContent { NoActionPreview() }
        compose.onNodeWithText("Well, this is awkward").assertExists()
    }

    @Test
    fun withActionPreviewRenders() {
        compose.setContent { WithActionPreview() }
        compose.onNodeWithText("Retry").assertExists()
    }
}
