package eu.kanade.tachiyomi.ui.browse.feed

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.source.Source
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import uy.kohesive.injekt.api.get

internal fun FeedScreenModel.getEnabledSources(): List<Source> {
    val languages = sourcePreferences.enabledLanguages.get()
    val pinnedSources = sourcePreferences.pinnedSources.get()
    val disabledSources = sourcePreferences.disabledSources.get()
        .mapNotNull { it.toLongOrNull() }

    val list = sourceManager.getVisibleSources()
        .filter { it.lang in languages }
        .filterNot { it.id in disabledSources }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { "(${it.lang}) ${it.name}" })

    return list.sortedBy { it.id.toString() !in pinnedSources }
}

internal suspend fun FeedScreenModel.getSourceSavedSearches(
    sourceId: Long,
): List<SavedSearch> = getSavedSearchBySourceId.await(sourceId)

internal fun FeedScreenModel.createFeed(source: Source, savedSearch: SavedSearch?) {
    screenModelScope.launchNonCancellable {
        insertFeedSavedSearch.await(
            FeedSavedSearch(
                id = -1,
                source = source.id,
                savedSearch = savedSearch?.id,
                global = true,
            ),
        )
    }
}

internal fun FeedScreenModel.deleteFeed(feed: FeedSavedSearch) {
    screenModelScope.launchNonCancellable {
        deleteFeedSavedSearchById.await(feed.id)
    }
}
