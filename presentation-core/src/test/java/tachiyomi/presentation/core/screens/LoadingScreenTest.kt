package tachiyomi.presentation.core.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LoadingScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersAProgressIndicator() {
        compose.setContent { MaterialTheme { LoadingScreen() } }
        compose.onRoot().assertExists()
    }

    @Test
    fun acceptsAModifier() {
        compose.setContent { MaterialTheme { LoadingScreen(modifier = Modifier.testTag("loading")) } }
        compose.onNodeWithTag("loading").assertExists()
    }
}
