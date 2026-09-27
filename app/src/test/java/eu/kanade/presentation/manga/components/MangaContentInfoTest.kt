package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.browse.UiDispatcherReset
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaContentInfoTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    private fun show(title: String, author: String?, artist: String?) {
        compose.setContent {
            MaterialTheme {
                Column {
                    MangaContentInfo(
                        title = title,
                        author = author,
                        artist = artist,
                        status = 0L,
                        sourceName = "Src",
                        isStubSource = false,
                        doSearch = { query, global -> events += "$query $global" },
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun blankTitleAndAuthor() {
        show(title = " ", author = " ", artist = " ")
        compose.onNodeWithText("Unknown title").performClick()
        compose.onNodeWithText("Unknown title").performTouchInput { longClick() }
        compose.onNodeWithText("Unknown author").performClick()
        compose.onNodeWithText("Unknown author").performTouchInput { longClick() }
        events shouldContainExactly emptyList()
    }

    @Test
    fun missingAuthorWithArtist() {
        show(title = "T", author = null, artist = "Art")
        compose.onNodeWithText("Art").performClick()
        compose.onNodeWithText("Art").performTouchInput { longClick() }
        compose.onNodeWithText("Unknown author").performClick()
        events shouldContainExactly listOf("Art true")
    }

    @Test
    fun sameAuthorAndArtist() {
        show(title = "T", author = "Same", artist = "Same")
        compose.onAllNodesWithText("Same").fetchSemanticsNodes().size shouldBe 1
        compose.onNodeWithText("Same").performTouchInput { longClick() }
    }

    @Test
    fun nullArtist() {
        show(title = "T", author = "Ann", artist = null)
        compose.onNodeWithText("Ann").performClick()
        events shouldContainExactly listOf("Ann true")
    }
}
