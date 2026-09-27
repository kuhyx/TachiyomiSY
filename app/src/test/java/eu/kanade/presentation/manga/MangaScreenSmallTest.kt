package eu.kanade.presentation.manga

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.data.download.model.Download
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class MangaScreenSmallTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val host = MangaScreenHost(compose)

    @Before
    fun setUp() = host.start()

    @After
    fun tearDown() = host.stop()

    private fun chapters() = listOf(
        chapterItem(mangaChapter(1L, lastPageRead = 3L)),
        chapterItem(mangaChapter(2L, read = true, lastPageRead = 3L, dateUpload = 1_000L)),
        chapterItem(mangaChapter(3L, bookmark = true, scanlator = "Group"), showScanlator = true),
        chapterItem(mangaChapter(6L), state = Download.State.DOWNLOADED, sourceName = "Other"),
    )

    @Test
    fun headerActionsForward() {
        host.show(screenState(chapters = chapters()))
        compose.onNodeWithText("Add to library").performClick()
        compose.onNodeWithText("N/A").performClick()
        compose.onNodeWithText("Tracking").performClick()
        compose.onNodeWithText("WebView").performClick()
        compose.onNodeWithText("WebView").performTouchInput { longClick() }
        compose.onNodeWithText("Merge").performClick()
        compose.onNodeWithText("See Recommendations").performClick()
        compose.onNodeWithContentDescription("Cover").performClick()
        host.events shouldContainExactly listOf(
            "favorite",
            "interval",
            "tracking",
            "webview",
            "webview long",
            "merge",
            "recommend",
            "cover",
        )
    }

    @Test
    fun titlesSearch() {
        host.show(screenState())
        compose.onAllNodesWithText("Needle")[0].performClick()
        compose.onNodeWithText("Ann").performClick()
        compose.onNodeWithText("Bob").performClick()
        compose.onNodeWithText("Plain").performClick()
        compose.onAllNodesWithText("Needle")[0].performTouchInput { longClick() }
        host.events shouldContainExactly listOf(
            "search Needle true",
            "search Ann true",
            "search Bob true",
            "search Plain false",
        )
    }

    @Test
    fun chapterRowsForward() {
        host.show(screenState(chapters = chapters()))
        compose.onNodeWithText("Chapter 1").performClick()
        compose.onNodeWithText("Chapter 1").performTouchInput { longClick() }
        compose.onNodeWithText("Resume", useUnmergedTree = true).performClick()
        compose.onNodeWithText("4 chapters").performClick()
        compose.onNodeWithText("Page: 4").assertExists()
        compose.onNodeWithText("Group").assertExists()
        compose.onNodeWithText("Other").assertExists()
        compose.onAllNodesWithText("Missing 2 chapters").fetchSemanticsNodes().size shouldBe 2
        host.events shouldContainExactly listOf("open 1", "select 1 true true", "continue", "filter")
    }

    @Test
    fun toolbarForwards() {
        host.show(screenState(chapters = chapters()))
        host.click("Download", lowest = false)
        compose.onNodeWithText("Unread").performClick()
        compose.onNodeWithContentDescription("Filter").performClick()
        listOf("Refresh", "Edit categories", "Migrate", "Share", "Notes").forEach {
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithText(it).performClick()
        }
        compose.onNodeWithContentDescription("Navigate up").performClick()
        host.events shouldContainExactly listOf(
            "download UNREAD_CHAPTERS",
            "filter",
            "refresh",
            "category",
            "migrate",
            "share",
            "notes",
            "up",
        )
    }

    @Test
    fun optionalActionsHidden() {
        host.actions = host.recorder.build(optional = false)
        host.show(screenState(chapters = listOf(chapterItem(mangaChapter(1L)))))
        compose.onNodeWithText("WebView").assertDoesNotExist()
        host.count("Download") shouldBe 1
        compose.onNodeWithText("N/A").performClick()
        compose.onNodeWithText("Start", useUnmergedTree = true).assertExists()
        host.events shouldContainExactly emptyList()
    }

    @Test
    fun overflowInstead() {
        val state = screenState(
            manga = screenManga(source = MERGED_SOURCE_ID, favorite = true),
            overflow = true,
            mergeWithAnother = true,
        )
        host.show(state)
        compose.onNodeWithText("Merge").assertDoesNotExist()
        compose.onNodeWithText("Merge With Another").performClick()
        listOf("Merge", "Edit info", "See Recommendations", "Merge settings").forEach {
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithText(it).performClick()
        }
        compose.onNodeWithText("In library").assertExists()
        host.events shouldContainExactly listOf("merge another", "merge", "edit info", "recommend", "merged settings")
    }

    @Test
    fun nextUpdateLabels() {
        host.nextUpdate = Instant.now().plus(3, ChronoUnit.DAYS).plusSeconds(60)
        host.show(screenState(manga = screenManga(fetchInterval = -2)))
        compose.onNodeWithText("3 days").assertExists()
        host.nextUpdate = Instant.now().minusSeconds(60)
        compose.waitForIdle()
        compose.onNodeWithText("Soon").assertExists()
        compose.onAllNodesWithContentDescription("Cover").fetchSemanticsNodes().size shouldBe 1
    }
}
