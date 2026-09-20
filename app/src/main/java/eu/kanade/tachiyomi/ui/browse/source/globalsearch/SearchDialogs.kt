package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel.Dialog
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal fun SearchScreenModel.updateSearchQuery(query: String?) {
    updateState { it.copy(searchQuery = query) }
}

internal fun SearchScreenModel.setSourceFilter(filter: SourceFilter) {
    updateState { it.copy(sourceFilter = filter) }
    search()
}

internal fun SearchScreenModel.toggleFilterResults() {
    preferences.globalSearchFilterState.toggle()
}

internal fun SearchScreenModel.setMigrateDialog(currentId: Long, target: Manga) {
    screenModelScope.launchIO {
        val current = getManga.await(currentId)
        if (current != null) {
            updateState { it.copy(dialog = Dialog.Migrate(target, current)) }
        }
    }
}

internal fun SearchScreenModel.clearDialog() {
    updateState { it.copy(dialog = null) }
}
