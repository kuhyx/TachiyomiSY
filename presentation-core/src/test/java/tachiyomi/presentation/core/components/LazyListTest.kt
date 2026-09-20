package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val LIST = "list"
private const val ITEMS = 60

/** The two LazyColumn wrappers: a scrollbar one and a fast-scroller one, both filling in their defaults. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class LazyListTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyListState()
    private var tick by mutableIntStateOf(0)
    private var isScrollEnabled by mutableStateOf(true)

    private val rows: LazyListScope.() -> Unit = {
        stickyHeader(key = "sticky:0") { _ -> Text(text = "Header") }
        items(ITEMS) { Text(text = "Row $it", modifier = Modifier.height(40.dp)) }
    }

    @Test
    fun scrollbarColumnDefaults() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    ScrollbarLazyColumn(modifier = Modifier.height(300.dp), content = rows)
                }
            }
        }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
    }

    @Test
    fun scrollbarColumnEveryOption() {
        compose.setContent {
            MaterialTheme {
                ScrollbarLazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp).testTag(LIST),
                    state = state,
                    contentPadding = PaddingValues(8.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    userScrollEnabled = isScrollEnabled,
                    content = rows,
                )
            }
        }
        compose.onNodeWithTag(LIST).assertHeightIsEqualTo(300.dp)
        compose.runOnIdle { isScrollEnabled = false }
        compose.onNodeWithTag(LIST).assertHeightIsEqualTo(300.dp)
        state.layoutInfo.reverseLayout shouldBe true
    }

    @Test
    fun scrollbarColumnReversedDefault() {
        compose.setContent {
            MaterialTheme {
                ScrollbarLazyColumn(modifier = Modifier.height(300.dp), reverseLayout = true, content = rows)
            }
        }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
    }

    @Test
    fun fastScrollColumnDefaults() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    FastScrollLazyColumn(modifier = Modifier.height(300.dp), content = rows)
                }
            }
        }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
        compose.runOnIdle { tick += 1 }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
    }

    @Test
    fun fastScrollColumnEveryOption() {
        compose.setContent {
            MaterialTheme {
                FastScrollLazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp).testTag(LIST),
                    state = state,
                    contentPadding = PaddingValues(8.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    userScrollEnabled = isScrollEnabled,
                    content = rows,
                )
            }
        }
        compose.onNodeWithTag(LIST).assertHeightIsEqualTo(300.dp)
        compose.runOnIdle { isScrollEnabled = false }
        compose.onNodeWithTag(LIST).assertHeightIsEqualTo(300.dp)
        state.layoutInfo.reverseLayout shouldBe true
    }

    @Test
    fun fastColumnReversedDefault() {
        compose.setContent {
            MaterialTheme {
                FastScrollLazyColumn(modifier = Modifier.height(300.dp), reverseLayout = true, content = rows)
            }
        }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
    }
}
