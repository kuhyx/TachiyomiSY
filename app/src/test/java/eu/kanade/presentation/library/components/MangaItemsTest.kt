package eu.kanade.presentation.library.components

import eu.kanade.presentation.util.invokeClick
import org.junit.Before
import org.junit.After
import eu.kanade.presentation.util.PresentationKoin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.MangaCover

@RunWith(RobolectricTestRunner::class)
internal class MangaItemsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private val cover by lazy {
        MangaCover(mangaId = 1L, sourceId = 2L, isMangaFavorite = true, ogUrl = null, lastModified = 0L)
    }

    @Test
    fun defaultItemsRender() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaCompactGridItem(coverData = cover, onClick = {}, onLongClick = {})
                    MangaComfortableGridItem(coverData = cover, title = "Comfy", onClick = {}, onLongClick = {})
                    MangaListItem(coverData = cover, title = "Row", onClick = {}, onLongClick = {}) {}
                    MangaGridCover()
                    Box { CoverTextOverlay(title = "Overlay") }
                    GridItemTitle(title = "Plain", style = MaterialTheme.typography.titleSmall, minLines = 1)
                }
            }
        }
        compose.onNodeWithText("Comfy").assertExists()
        compose.onNodeWithText("Overlay").assertExists()
        compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun selectedItemsWithEverything() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaCompactGridItem(
                        coverData = cover,
                        onClick = { events += "compact" },
                        onLongClick = {},
                        isSelected = true,
                        title = "Compact",
                        onClickContinueReading = { events += "continue compact" },
                        coverAlpha = 0.5f,
                        coverBadgeStart = { Text("start") },
                        coverBadgeEnd = { Text("end") },
                    )
                    MangaComfortableGridItem(
                        coverData = cover,
                        title = "Comfy",
                        onClick = {},
                        onLongClick = {},
                        isSelected = true,
                        titleMaxLines = 3,
                        coverAlpha = 0.5f,
                        coverBadgeStart = { Text("start2") },
                        coverBadgeEnd = { Text("end2") },
                        onClickContinueReading = { events += "continue comfy" },
                    )
                    MangaListItem(
                        coverData = cover,
                        title = "Row",
                        onClick = {},
                        onLongClick = {},
                        isSelected = true,
                        coverAlpha = 0.5f,
                        onClickContinueReading = { events += "continue row" },
                    ) { Text("badge") }
                }
            }
        }
        compose.onNodeWithText("Compact").performClick()
        val resume = compose.onAllNodesWithContentDescription("Resume")
        resume[0].invokeClick()
        resume[1].invokeClick()
        resume[2].invokeClick()
        events shouldContainExactly listOf("compact", "continue compact", "continue comfy", "continue row")
    }

    @Test
    fun badgesShowOnlyWhenRelevant() {
        compose.setContent {
            MaterialTheme {
                Column {
                    androidx.compose.foundation.layout.Row {
                        DownloadsBadge(count = 0)
                        UnreadBadge(count = 0)
                        LanguageBadge(isLocal = false, sourceLanguage = "")
                        LanguageBadge(isLocal = false, sourceLanguage = "ja")
                    }
                    BadgePreview()
                    GlobalSearchItem(searchQuery = "q", onClick = { events += "global" })
                }
            }
        }
        compose.onNodeWithText("JA").assertExists()
        compose.onNodeWithText("globally", substring = true).performClick()
        events shouldContainExactly listOf("global")
    }
}
