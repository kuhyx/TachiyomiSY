package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.source.getMainSource
import exh.util.nullIfBlank
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

// FilterList never equals another one (see FilterList.equals), so the filters always travel along.
internal fun SourceFeedScreenModel.onFilter(onBrowseClick: (query: String?, filters: String?) -> Unit) {
    screenModelScope.launchIO {
        dismissDialog()
        onBrowseClick(
            state.value.searchQuery?.nullIfBlank(),
            Json.encodeToString(filterSerializer.serialize(state.value.filters)),
        )
    }
}

internal fun SourceFeedScreenModel.onSavedSearch(
    search: EXHSavedSearch,
    onBrowseClick: (query: String?, searchId: Long) -> Unit,
    onToast: (StringResource) -> Unit,
) {
    screenModelScope.launchIO {
        doOnSavedSearch(search = search, onBrowseClick = onBrowseClick, onToast = onToast)
    }
}

private suspend fun SourceFeedScreenModel.doOnSavedSearch(
    search: EXHSavedSearch,
    onBrowseClick: (query: String?, searchId: Long) -> Unit,
    onToast: (StringResource) -> Unit,
) {
    if (search.filterList == null && state.value.filters.isNotEmpty()) {
        withUIContext {
            onToast(SYMR.strings.save_search_invalid)
        }
        return
    }

    // A saved search's FilterList never equals the source's (see FilterList.equals): it always applies.
    dismissDialog()
    onBrowseClick(
        state.value.searchQuery?.nullIfBlank(),
        search.id,
    )
}

internal fun SourceFeedScreenModel.onSavedSearchAddToFeed(
    search: EXHSavedSearch,
    onToast: (StringResource) -> Unit,
) {
    screenModelScope.launchIO { doOnSavedSearchAddToFeed(search = search, onToast = onToast) }
}

private suspend fun SourceFeedScreenModel.doOnSavedSearchAddToFeed(
    search: EXHSavedSearch,
    onToast: (StringResource) -> Unit,
) {
    if (hasTooManyFeeds()) {
        withUIContext {
            onToast(SYMR.strings.too_many_in_feed)
        }
        return
    }
    openAddFeed(search.id, search.name)
}

internal fun SourceFeedScreenModel.onMangaDexRandom(onRandomFound: (String) -> Unit) {
    screenModelScope.launchIO { doOnMangaDexRandom(onRandomFound = onRandomFound) }
}

private suspend fun SourceFeedScreenModel.doOnMangaDexRandom(onRandomFound: (String) -> Unit) {
    val random = source.getMainSource<MangaDex>()?.fetchRandomMangaUrl()
        ?: return
    onRandomFound(random)
}
