package tachiyomi.presentation.core.util

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The IDE previews are plain composables: both compose, draw and survive a parent recomposition. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class ScrollbarPreviewsTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var host: View? = null
    private var draws = 0

    private val counted = Modifier.drawWithContent {
        drawContent()
        draws += 1
    }

    @Test
    fun columnPreview() {
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = counted) { LazyListScrollbarPreview() }
                }
            }
        }
        compose.onNodeWithText("Item 1").assertIsDisplayed()
        compose.forceDraw(checkNotNull(host))
        draws shouldBeGreaterThan 0
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("Item 1").assertIsDisplayed()
    }

    @Test
    fun rowPreview() {
        compose.setContent {
            host = LocalView.current
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Box(modifier = counted) { LazyRowScrollbarPreview() }
                }
            }
        }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.forceDraw(checkNotNull(host))
        draws shouldBeGreaterThan 0
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("1").assertIsDisplayed()
    }
}
