package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.paging.filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.source.model.Filter as SourceModelFilter

internal fun BrowseSourceScreenModel.resetFilters() {
    updateState { it.copy(filters = source.getFilterList()) }
}

internal fun BrowseSourceScreenModel.setListing(listing: Listing) {
    updateState { it.copy(listing = listing, toolbarQuery = null) }
}

internal fun BrowseSourceScreenModel.setFilters(filters: FilterList) {
    updateState {
        it.copy(
            filters = filters,
        )
    }
}

internal fun BrowseSourceScreenModel.search(query: String? = null, filters: FilterList? = null) {
    // SY -->
    if (filters != null && filters !== state.value.filters) {
        updateState { state -> state.copy(filters = filters) }
    }
    // SY <--
    val input = state.value.listing as? Listing.Search
        ?: Listing.Search(query = null, filters = source.getFilterList())

    updateState {
        it.copy(
            listing = input.copy(
                query = query ?: input.query,
                filters = filters ?: input.filters,
            ),
            toolbarQuery = query ?: input.query,
        )
    }
}

internal fun BrowseSourceScreenModel.searchGenre(genreName: String) {
    val defaultFilters = source.getFilterList()
    val genreExists = defaultFilters.any { applyGenre(it, genreName) }

    updateState {
        val listing = if (genreExists) {
            Listing.Search(query = null, filters = defaultFilters)
        } else {
            Listing.Search(query = genreName, filters = defaultFilters)
        }
        it.copy(
            filters = defaultFilters,
            listing = listing,
            toolbarQuery = listing.query,
        )
    }
}

// Selects [genreName] in [sourceFilter] when it offers it; true when something was selected.
private fun applyGenre(sourceFilter: SourceModelFilter<*>, genreName: String): Boolean = when (sourceFilter) {
    is SourceModelFilter.Group<*> -> {
        val filter = sourceFilter.state
            .filterIsInstance<SourceModelFilter<*>>()
            .firstOrNull { it.name.equals(genreName, true) }
        when (filter) {
            is SourceModelFilter.TriState -> {
                filter.state = 1
            }
            is SourceModelFilter.CheckBox -> {
                filter.state = true
            }
            else -> {}
        }
        filter != null
    }
    is SourceModelFilter.Select<*> -> {
        val index = sourceFilter.values.filterIsInstance<String>().indexOfFirst { it.equals(genreName, true) }
        if (index != -1) sourceFilter.state = index
        index != -1
    }
    else -> {
        false
    }
}
