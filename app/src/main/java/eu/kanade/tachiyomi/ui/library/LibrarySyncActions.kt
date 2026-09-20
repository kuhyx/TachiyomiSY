package eu.kanade.tachiyomi.ui.library

import cafe.adriel.voyager.core.model.screenModelScope
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/** Returns first unread chapter of a manga. */
internal suspend fun LibraryScreenModel.getFirstUnread(manga: Manga): Chapter? =
    getNextChapters.await(manga.id).firstOrNull()

internal fun LibraryScreenModel.runRecommendationSearch(selection: List<Manga>) {
    recommendationSearch.runSearch(screenModelScope, selection)?.let {
        recommendationSearchJob = it
    }
}

internal fun LibraryScreenModel.cancelRecommendationSearch() {
    recommendationSearchJob?.cancel()
}

internal fun LibraryScreenModel.runSync() {
    favoritesSync.runSync(screenModelScope)
}

internal fun LibraryScreenModel.onAcceptSyncWarning() {
    exhPreferences.exhShowSyncIntro.set(false)
}
