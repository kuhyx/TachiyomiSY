package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class MangaSmallComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun statusPresentations() {
        mangaStatusPresentation(SManga.ONGOING.toLong()).label shouldBe MR.strings.ongoing
        mangaStatusPresentation(SManga.ON_HIATUS.toLong()).label shouldBe MR.strings.on_hiatus
        mangaStatusPresentation(99L).label shouldBe MR.strings.unknown
        ChapterDownloadAction.entries.size shouldBe 4
    }

    @Test
    fun separatorsAndMissingCount() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Row {
                        DotSeparatorText()
                        DotSeparatorText(modifier = Modifier)
                        DotSeparatorNoSpaceText()
                        DotSeparatorNoSpaceText(modifier = Modifier)
                    }
                    MissingChapterCountListItem(count = 1, modifier = Modifier)
                    Preview()
                }
            }
        }
        compose.onNodeWithText("Missing 1 chapter").assertExists()
        compose.onNodeWithText("Missing 42 chapters").assertExists()
    }

    @Test
    fun infoButtonsForward() {
        compose.setContent {
            MaterialTheme {
                MangaInfoButtons(
                    showRecommendsButton = true,
                    showMergeWithAnotherButton = true,
                    onRecommendClicked = { events += "recommend" },
                    onMergeWithAnotherClicked = { events += "merge" },
                )
            }
        }
        compose.onNodeWithText("See Recommendations").performClick()
        compose.onNodeWithText("Merge With Another").performClick()
        events shouldContainExactly listOf("recommend", "merge")
    }

    @Test
    fun infoButtonsCanBeHidden() {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaInfoButtons(
                        showRecommendsButton = false,
                        showMergeWithAnotherButton = false,
                        onRecommendClicked = {},
                        onMergeWithAnotherClicked = {},
                    )
                    MangaInfoButtons(
                        showRecommendsButton = false,
                        showMergeWithAnotherButton = true,
                        onRecommendClicked = {},
                        onMergeWithAnotherClicked = {},
                    )
                }
            }
        }
        compose.onNodeWithText("See Recommendations").assertDoesNotExist()
    }
}
