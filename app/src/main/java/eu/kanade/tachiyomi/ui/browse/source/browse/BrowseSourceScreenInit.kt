package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.paging.map
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import tachiyomi.domain.source.model.EXHSavedSearch
import uy.kohesive.injekt.api.get

internal fun BrowseSourceScreenModel.restoreListing() {
    updateState {
        var query: String? = null
        var listing = it.listing

        if (listing is Listing.Search) {
            query = listing.query
            listing = Listing.Search(query, source.getFilterList())
        }

        it.copy(
            listing = listing,
            filters = source.getFilterList(),
            toolbarQuery = query,
        )
    }
}

internal fun BrowseSourceScreenModel.rememberLastUsedSource() {
    if (!getIncognitoState.await(source.id)) {
        sourcePreferences.lastUsedSource.set(source.id)
    }
}

// SY -->
internal fun BrowseSourceScreenModel.applyInitialSearch() {
    val savedSearchFilters = savedSearch
    val jsonFilters = filtersJson
    val filters = state.value.filters
    if (savedSearchFilters != null) {
        val savedSearch = runBlocking { getExhSavedSearch.awaitOne(savedSearchFilters) { filters } }
        if (savedSearch != null) {
            search(query = savedSearch.query, filters = savedSearch.filterList)
        }
    } else if (jsonFilters != null) {
        runCatching {
            val filtersJson = Json.decodeFromString<JsonArray>(jsonFilters)
            filterSerializer.deserialize(filters, filtersJson)
            search(filters = filters)
        }
    }
}

internal fun BrowseSourceScreenModel.observeSavedSearches() {
    getExhSavedSearch.subscribe(source.id, source::getFilterList)
        .map { it.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, EXHSavedSearch::name)) }
        .onEach { savedSearches ->
            updateState { it.copy(savedSearches = savedSearches) }
        }
        .launchIn(screenModelScope)
}

internal fun BrowseSourceScreenModel.getColumnsPreference(orientation: Int): GridCells {
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) {
        libraryPreferences.landscapeColumns
    } else {
        libraryPreferences.portraitColumns
    }.get()
    return if (columns == 0) GridCells.Adaptive(128.dp) else GridCells.Fixed(columns)
}
