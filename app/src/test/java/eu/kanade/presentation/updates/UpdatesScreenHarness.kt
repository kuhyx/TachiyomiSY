package eu.kanade.presentation.updates

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.ComposeContentTestRule
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel

/** Composes [UpdateScreen] recording every callback as a readable line in [events]. */
internal class UpdatesScreenHarness(private val compose: ComposeContentTestRule) {
    val events: MutableList<String> = mutableListOf()
    var updateStarts: Boolean = true
    var dispatcher: OnBackPressedDispatcher? = null

    private fun UpdatesItem.id(): Long = update.mangaId

    private fun List<UpdatesItem>.ids(): List<Long> = map { it.id() }

    fun show(
        state: UpdatesScreenModel.State,
        preserveReadingPosition: Boolean = false,
        hasActiveFilters: Boolean = false,
    ) {
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            MaterialTheme {
                UpdateScreen(
                    state = state,
                    snackbarHostState = SnackbarHostState(),
                    lastUpdated = 0L,
                    preserveReadingPosition = preserveReadingPosition,
                    onClickCover = { events += "cover ${it.id()}" },
                    onSelectAll = { events += "selectAll $it" },
                    onInvertSelection = { events += "invert" },
                    onCalendarClicked = { events += "calendar" },
                    onUpdateLibrary = {
                        events += "update"
                        updateStarts
                    },
                    onDownloadChapter = { items, action -> events += "download ${items.ids()} $action" },
                    onMultiBookmarkClicked = { items, bookmark -> events += "bookmark ${items.ids()} $bookmark" },
                    onMultiMarkAsReadClicked = { items, read -> events += "read ${items.ids()} $read" },
                    onMultiDeleteClicked = { events += "delete ${it.ids()}" },
                    onUpdateSelected = { item, selected, long -> events += "select ${item.id()} $selected $long" },
                    onOpenChapter = { events += "open ${it.id()}" },
                    onFilterClicked = { events += "filter" },
                    hasActiveFilters = hasActiveFilters,
                )
            }
        }
        compose.waitForIdle()
    }
}
