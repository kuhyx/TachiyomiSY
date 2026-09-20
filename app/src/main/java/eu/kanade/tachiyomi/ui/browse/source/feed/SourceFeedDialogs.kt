package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.ui.browse.source.feed.SourceFeedScreenModel.Dialog
import tachiyomi.domain.source.model.FeedSavedSearch
import uy.kohesive.injekt.api.get

internal fun SourceFeedScreenModel.search(query: String?) {
    updateState { it.copy(searchQuery = query) }
}

internal fun SourceFeedScreenModel.openFilterSheet() {
    updateState { it.copy(dialog = Dialog.Filter) }
}

internal fun SourceFeedScreenModel.openDeleteFeed(feed: FeedSavedSearch) {
    updateState { it.copy(dialog = Dialog.DeleteFeed(feed)) }
}

internal fun SourceFeedScreenModel.openAddFeed(feedId: Long, name: String) {
    updateState { it.copy(dialog = Dialog.AddFeed(feedId, name)) }
}

internal fun SourceFeedScreenModel.dismissDialog() {
    updateState { it.copy(dialog = null) }
}
