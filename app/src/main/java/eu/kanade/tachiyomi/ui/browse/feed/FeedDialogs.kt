package eu.kanade.tachiyomi.ui.browse.feed

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenModel.Dialog
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenModel.Event
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.model.FeedSavedSearch
import uy.kohesive.injekt.api.get

internal fun FeedScreenModel.openAddDialog() {
    screenModelScope.launchIO { doOpenAddDialog() }
}

private suspend fun FeedScreenModel.doOpenAddDialog() {
    if (hasTooManyFeeds()) {
        emit(Event.TooManyFeeds)
        return
    }
    updateState { state ->
        state.copy(
            dialog = Dialog.AddFeed(getEnabledSources()),
        )
    }
}

internal fun FeedScreenModel.openAddSearchDialog(source: Source) {
    screenModelScope.launchIO {
        val searches = (if (source.supportsLatest) listOf(null) else emptyList()) + getSourceSavedSearches(source.id)
        updateState { it.copy(dialog = Dialog.AddFeedSearch(source, searches)) }
    }
}

internal fun FeedScreenModel.openDeleteDialog(feed: FeedSavedSearch) {
    screenModelScope.launchIO {
        updateState { state ->
            state.copy(
                dialog = Dialog.DeleteFeed(feed),
            )
        }
    }
}

internal fun FeedScreenModel.dismissDialog() {
    updateState { it.copy(dialog = null) }
}
