package eu.kanade.presentation.track

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.util.tapOutsidePopup
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class TrackerSearchResultsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun show(result: Result<List<TrackSearch>>?, selected: TrackSearch? = null) {
        compose.setContent {
            MaterialTheme {
                TrackerSearchResults(
                    queryResult = result,
                    selected = selected,
                    onSelectedChange = { events += "select ${it.title}" },
                    innerPadding = PaddingValues(),
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun failureShowsItsMessage() {
        show(Result.failure(IllegalStateException("tracker down")))
        compose.onNodeWithText("tracker down").assertExists()
    }

    @Test
    fun failureWithoutMessage() {
        show(Result.failure(IllegalStateException()))
        compose.onNodeWithText("Unknown error").assertExists()
    }

    @Test
    fun emptyResults() {
        show(Result.success(emptyList()))
        compose.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun resultsShowTheirDetails() {
        val picked = trackSearch("Picked")
        show(Result.success(listOf(picked, trackSearch("Other", summary = " ", score = -1.0))), selected = picked)
        compose.onNodeWithText("A summary.").assertExists()
        compose.onNodeWithText("Author, Artist").assertExists()
        compose.onNodeWithText("Manga").assertExists()
        compose.onNodeWithText("Finished").assertExists()
        compose.onNodeWithText("7.5").assertExists()
        compose.onNodeWithText("Other").performClick()
        events shouldContainExactly listOf("select Other")
    }

    @Test
    fun longPressMenuCopiesTheTitle() {
        show(Result.success(listOf(trackSearch("Copied"))))
        compose.onNodeWithText("Copied").performTouchInput { longClick() }
        compose.onNodeWithText("Copy to clipboard").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Copy to clipboard").assertDoesNotExist()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text shouldBe "Copied"
    }

    @Test
    fun longPressMenuOpensTheBrowser() {
        show(Result.success(listOf(trackSearch("Linked"), trackSearch("Unlinked", trackingUrl = " "))))
        compose.onNodeWithText("Linked").performTouchInput { longClick() }
        compose.onNodeWithText("Open in browser").performClick()
        compose.tapOutsidePopup()
        compose.onNodeWithText("Unlinked").performTouchInput { longClick() }
        compose.onNodeWithText("Open in browser").performClick()
        compose.tapOutsidePopup()
        shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .nextStartedActivity?.data?.toString() shouldBe "https://example.com/t"
    }

    @Test
    fun resultsWithoutCreators() {
        val bare = trackSearch("Bare").also {
            it.authors = emptyList()
            it.artists = emptyList()
            it.publishingType = ""
        }
        show(Result.success(listOf(bare)))
        compose.onNodeWithText("Type").assertDoesNotExist()
    }

    @Test
    fun artistsAloneStillShow() {
        val artistOnly = trackSearch("Drawn").also { it.authors = emptyList() }
        show(Result.success(listOf(artistOnly)))
        compose.onNodeWithText("Author, Artist").assertExists()
    }

    @Test
    fun previewsRender() {
        val previews = TrackerSearchPreviewProvider().values.toList()
        compose.setContent { previews.forEach { TrackerSearchPreviews(it) } }
        compose.waitForIdle()
        compose.onAllNodesWithText("search text").fetchSemanticsNodes().size shouldBe 2
    }
}
