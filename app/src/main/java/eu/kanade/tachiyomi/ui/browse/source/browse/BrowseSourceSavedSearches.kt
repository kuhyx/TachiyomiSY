package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.paging.map
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Dialog
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import exh.source.getMainSource
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

// EXH -->
internal fun BrowseSourceScreenModel.onSaveSearch() {
    screenModelScope.launchIO {
        val names = state.value.savedSearches.map { it.name }
        updateState { it.copy(dialog = Dialog.CreateSavedSearch(names)) }
    }
}

internal fun BrowseSourceScreenModel.onSavedSearch(
    search: EXHSavedSearch,
    onToast: (StringResource) -> Unit,
) {
    screenModelScope.launchIO { doOnSavedSearch(search = search, onToast = onToast) }
}

private suspend fun BrowseSourceScreenModel.doOnSavedSearch(search: EXHSavedSearch, onToast: (StringResource) -> Unit) {
    if (search.filterList == null && state.value.filters.isNotEmpty()) {
        withUIContext {
            onToast(SYMR.strings.save_search_invalid)
        }
        return
    }

    val allDefault = search.filterList != null && search.filterList == source.getFilterList()
    setDialog(null)

    val filters = search.filterList
        ?.takeUnless { allDefault }
        ?: source.getFilterList()

    updateState {
        it.copy(
            listing = Listing.Search(
                query = search.query,
                filters = filters,
            ),
            filters = filters,
            toolbarQuery = search.query,
        )
    }
}

internal fun BrowseSourceScreenModel.onSavedSearchPress(search: EXHSavedSearch) {
    updateState { it.copy(dialog = Dialog.DeleteSavedSearch(search.id, search.name)) }
}

internal fun BrowseSourceScreenModel.saveSearch(
    name: String,
) {
    screenModelScope.launchNonCancellable {
        val query = state.value.toolbarQuery?.takeUnless {
            it.isBlank() || it == GetRemoteManga.QUERY_POPULAR || it == GetRemoteManga.QUERY_LATEST
        }?.trim()
        val filterList = state.value.filters.ifEmpty { source.getFilterList() }
        insertSavedSearch.await(
            SavedSearch(
                id = -1,
                source = source.id,
                name = name.trim(),
                query = query,
                filtersJson = runCatching {
                    filterSerializer.serialize(filterList).ifEmpty { null }?.let { Json.encodeToString(it) }
                }.getOrNull(),
            ),
        )
    }
}

internal fun BrowseSourceScreenModel.deleteSearch(savedSearchId: Long) {
    screenModelScope.launchNonCancellable {
        deleteSavedSearchById.await(savedSearchId)
    }
}

internal fun BrowseSourceScreenModel.onMangaDexRandom(onRandomFound: (String) -> Unit) {
    screenModelScope.launchIO { doOnMangaDexRandom(onRandomFound = onRandomFound) }
}

private suspend fun BrowseSourceScreenModel.doOnMangaDexRandom(onRandomFound: (String) -> Unit) {
    val random = source.getMainSource<MangaDex>()?.fetchRandomMangaUrl()
        ?: return
    onRandomFound(random)
}
