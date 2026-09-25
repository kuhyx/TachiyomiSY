package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class GlobalSearchItemsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun cardRowShowsTitles() {
        val titles = listOf(
            Manga.create().copy(id = 1L, ogTitle = "Owned", favorite = true),
            Manga.create().copy(id = 2L, ogTitle = "New"),
        )
        compose.setContent {
            MaterialTheme {
                GlobalSearchCardRow(
                    titles = titles,
                    onClick = { events += "click ${it.id}" },
                    onLongClick = { events += "long ${it.id}" },
                    getManga = { remember(it) { mutableStateOf(it) } },
                )
            }
        }
        compose.onNodeWithText("Owned").performClick()
        compose.onNodeWithText("New").performTouchInput { longClick() }
        events shouldContainExactly listOf("click 1", "long 2")
    }

    @Test
    fun emptyCardRow() {
        compose.setContent {
            MaterialTheme {
                GlobalSearchCardRow(
                    titles = emptyList(),
                    onClick = {},
                    onLongClick = {},
                    getManga = { error("unused") },
                )
            }
        }
        compose.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun resultItemsClickAndLongClick() {
        compose.setContent {
            MaterialTheme {
                Column {
                    GlobalSearchResultItem(title = "Plain", subtitle = null, onClick = { events += "plain" }) {
                        GlobalSearchLoadingResultItem()
                    }
                    GlobalSearchResultItem(
                        title = "Long",
                        subtitle = "sub",
                        onClick = { events += "long click" },
                        modifier = Modifier,
                        onLongClick = { events += "long press" },
                    ) {
                        GlobalSearchErrorResultItem(message = null)
                        GlobalSearchErrorResultItem(message = "broken")
                        Text("inner")
                    }
                }
            }
        }
        compose.onNodeWithText("Unknown error").assertExists()
        compose.onNodeWithText("broken").assertExists()
        compose.onNodeWithText("Plain").performClick()
        compose.onNodeWithText("Long").performTouchInput { longClick() }
        compose.onNodeWithText("sub").performClick()
        events shouldContainExactly listOf("plain", "long press", "long click")
    }
}
