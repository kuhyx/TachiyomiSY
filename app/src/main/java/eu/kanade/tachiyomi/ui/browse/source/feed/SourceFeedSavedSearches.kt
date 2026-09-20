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

internal fun SourceFeedScreenModel.onFilter(onBrowseClick: (query: String?, filters: String?) -> Unit) {
    screenModelScope.launchIO {
        val allDefault = state.value.filters == source.getFilterList()
        dismissDialog()
        if (allDefault) {
            onBrowseClick(
                state.value.searchQuery?.nullIfBlank(),
                null,
            )
        } else {
            onBrowseClick(
                state.value.searchQuery?.nullIfBlank(),
                Json.encodeToString(filterSerializer.serialize(state.value.filters)),
            )
        }
    }
}

internal fun SourceFeedScreenModel.onSavedSearch(
    search: EXHSavedSearch,
    onBrowseClick: (query: String?, searchId: Long) -> Unit,
    onToast: (StringResource) -> Unit,
) {
    screenModelScope.launchIO {
        if (search.filterList == null && state.value.filters.isNotEmpty()) {
            withUIContext {
                onToast(SYMR.strings.save_search_invalid)
            }
            return@launchIO
        }

        val allDefault = search.filterList != null && search.filterList == source.getFilterList()
        dismissDialog()

        if (!allDefault) {
            onBrowseClick(
                state.value.searchQuery?.nullIfBlank(),
                search.id,
            )
        }
    }
}

internal fun SourceFeedScreenModel.onSavedSearchAddToFeed(
    search: EXHSavedSearch,
    onToast: (StringResource) -> Unit,
) {
    screenModelScope.launchIO {
        if (hasTooManyFeeds()) {
            withUIContext {
                onToast(SYMR.strings.too_many_in_feed)
            }
            return@launchIO
        }
        openAddFeed(search.id, search.name)
    }
}

internal fun SourceFeedScreenModel.onMangaDexRandom(onRandomFound: (String) -> Unit) {
    screenModelScope.launchIO {
        val random = source.getMainSource<MangaDex>()?.fetchRandomMangaUrl()
            ?: return@launchIO
        onRandomFound(random)
    }
}
