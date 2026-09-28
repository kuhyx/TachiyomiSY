package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LazyLibraryGridTest {
    @get:Rule
    val compose = createComposeRule()

    // The content type is what lets the grid reuse the item's composition; it must be the key itself.
    @Test
    fun searchItemTypeIsItsKey() {
        val state = LazyGridState()
        compose.setContent {
            MaterialTheme {
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = state) {
                    globalSearchItem(searchQuery = "query", onGlobalSearchClicked = {})
                }
            }
        }
        compose.runOnIdle {
            state.layoutInfo.visibleItemsInfo.single().contentType shouldBe "library_global_search_item"
        }
    }

    @Test
    fun adaptiveAndFixedColumns() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Box(modifier = Modifier.height(100.dp)) {
                        LazyLibraryGrid(columns = 0, contentPadding = PaddingValues()) {
                            item { Text("adaptive") }
                        }
                    }
                    LazyLibraryGrid(modifier = Modifier.height(100.dp), columns = 2, contentPadding = PaddingValues()) {
                        item { Text("fixed") }
                    }
                }
            }
        }
        compose.onNodeWithText("adaptive").assertExists()
        compose.onNodeWithText("fixed").assertExists()
    }
}
