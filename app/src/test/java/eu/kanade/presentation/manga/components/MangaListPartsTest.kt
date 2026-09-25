package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
internal class MangaListPartsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val manga = Manga.create().copy(id = 1L, ogTitle = "Listed")

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun baseItemDefaultsUseTheManga() {
        compose.setContent {
            MaterialTheme {
                Column {
                    BaseMangaListItem(manga = manga, onClickItem = { events += "item" })
                    BaseMangaListItem(
                        manga = manga,
                        modifier = Modifier,
                        onClickItem = {},
                        onClickCover = {},
                        cover = { Text("cover") },
                        actions = { Text("actions") },
                        content = { Text("content") },
                    )
                    BaseMangaListItem(manga = manga.copy(id = 2L, ogTitle = "Bare"))
                }
            }
        }
        compose.onNodeWithText("Listed").performClick()
        compose.onNodeWithText("Bare").performClick()
        compose.onNodeWithText("actions").assertExists()
        events shouldContainExactly listOf("item")
    }

    @Test
    fun chapterHeaderCounts() {
        compose.setContent {
            MaterialTheme {
                Column {
                    ChapterHeader(
                        enabled = true,
                        chapterCount = null,
                        missingChapterCount = 0,
                        onClick = { events += "h" },
                    )
                    ChapterHeader(
                        enabled = false,
                        chapterCount = 3,
                        missingChapterCount = 2,
                        onClick = {},
                        modifier = Modifier,
                    )
                }
            }
        }
        compose.onNodeWithText("3 chapters").assertExists()
        compose.onNodeWithText("Missing 2 chapters").assertExists()
        compose.onNodeWithText("Chapters").performClick()
        events shouldContainExactly listOf("h")
    }

    @Test
    fun notesFadeInOnChange() {
        var content by mutableStateOf("**bold**")
        compose.setContent { MaterialTheme { MangaNotesDisplay(content = content, modifier = Modifier) } }
        compose.onNodeWithText("bold").assertExists()
        content = "changed"
        compose.mainClock.advanceTimeBy(1_000L)
        compose.onNodeWithText("changed").assertExists()
    }
}
