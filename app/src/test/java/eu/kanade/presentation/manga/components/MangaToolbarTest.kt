package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.invokeClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaToolbarTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    @Test
    fun filteredToolbarDownloads() {
        compose.setContent {
            MaterialTheme {
                MangaToolbar(
                    title = "Needle",
                    hasFilters = true,
                    navigateUp = {},
                    onClickFilter = { events += "filter" },
                    onClickShare = null,
                    onClickDownload = { events += "download $it" },
                    onClickEditCategory = null,
                    onClickRefresh = {},
                    onClickMigrate = null,
                    onClickEditNotes = {},
                    onClickEditInfo = null,
                    onClickRecommend = null,
                    onClickMerge = null,
                    onClickMergedSettings = null,
                    actionModeCounter = 0,
                    onCancelActionMode = {},
                    onSelectAll = {},
                    onInvertSelection = {},
                    titleAlphaProvider = { 1f },
                    backgroundAlphaProvider = { 1f },
                )
            }
        }
        compose.onNodeWithContentDescription("Download").invokeClick()
        compose.waitForIdle()
        compose.onNodeWithText("Bookmarked").assertExists()
        compose.onNodeWithContentDescription("Download").invokeClick()
        compose.waitForIdle()
        compose.onNodeWithText("Bookmarked").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filter").invokeClick()
        events shouldContainExactly listOf("filter")
    }
}
