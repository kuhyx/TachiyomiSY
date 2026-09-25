package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.util.invokeClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val Overflow = listOf(
    "Clean titles",
    "Migrate",
    "Find recommendations",
    "Add to MangaDex follows",
    "Reset Info",
)

@RunWith(RobolectricTestRunner::class)
internal class LibraryBottomActionMenuTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun setMenu(overflow: Boolean = true, download: Boolean = true, visible: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                LibraryBottomActionMenu(
                    visible = visible,
                    onChangeCategoryClicked = { events += "category" },
                    onMarkAsReadClicked = { events += "read" },
                    onMarkAsUnreadClicked = { events += "unread" },
                    onDownloadClicked = { action: DownloadAction -> events += action.name }.takeIf { download },
                    onDeleteClicked = { events += "delete" },
                    onMigrateClicked = { events += "migrate" }.takeIf { overflow },
                    onClickCleanTitles = { events += "clean" }.takeIf { overflow },
                    onClickCollectRecommendations = { events += "recs" }.takeIf { overflow },
                    onClickAddToMangaDex = { events += "mangadex" }.takeIf { overflow },
                    onClickResetInfo = { events += "reset" }.takeIf { overflow },
                )
            }
        }
    }

    @Test
    fun buttonsForward() {
        setMenu(overflow = false)
        listOf("Set categories", "Delete", "Mark as read", "Mark as unread").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        compose.onNodeWithContentDescription("More").assertDoesNotExist()
        events shouldContainExactly listOf("category", "delete", "read", "unread")
    }

    @Test
    fun downloadOpensItsMenu() {
        setMenu()
        compose.onNodeWithContentDescription("Download").performClick()
        compose.onNodeWithText("Unread").performClick()
        events shouldContainExactly listOf(DownloadAction.UNREAD_CHAPTERS.name)
        compose.onNodeWithContentDescription("Download").performClick()
        compose.onNodeWithContentDescription("Download").invokeClick()
        compose.onNodeWithText("Unread").assertDoesNotExist()
    }

    @Test
    fun noCallbackNoDownloadButton() {
        setMenu(download = false)
        compose.onNodeWithContentDescription("Download").assertDoesNotExist()
    }

    @Test
    fun phonesMoveUnreadToOverflow() {
        setMenu()
        compose.onNodeWithContentDescription("Mark as unread").assertDoesNotExist()
        (listOf("Mark as unread") + Overflow).forEach { label ->
            compose.onNodeWithContentDescription("More").performClick()
            compose.onNodeWithText(label).performClick()
        }
        events shouldContainExactly listOf("unread", "clean", "migrate", "recs", "mangadex", "reset")
    }

    @Test
    @Config(qualifiers = "sw800dp")
    fun tabletsKeepUnreadInTheRow() {
        setMenu()
        compose.onNodeWithContentDescription("Mark as unread").performClick()
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Mark as unread").assertDoesNotExist()
        events shouldContainExactly listOf("unread")
    }

    @Test
    fun longPressConfirmsMore() {
        setMenu()
        compose.onNodeWithContentDescription("More").performTouchInput { longClick() }
        compose.onNodeWithText("More").assertExists()
    }

    @Test
    fun overflowEmptiness() {
        LibraryOverflowActions(null, null, null, null, null).isEmpty shouldBe true
        listOf(0, 1, 2, 3, 4).forEach { set ->
            val actions = List(5) { i -> {}.takeIf { i == set } }
            LibraryOverflowActions(
                onClickCleanTitles = actions[0],
                onMigrateClicked = actions[1],
                onClickCollectRecommendations = actions[2],
                onClickAddToMangaDex = actions[3],
                onClickResetInfo = actions[4],
            ).isEmpty shouldBe false
        }
    }

    @Test
    fun hiddenMenuShowsNothing() {
        setMenu(visible = false)
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }
}
