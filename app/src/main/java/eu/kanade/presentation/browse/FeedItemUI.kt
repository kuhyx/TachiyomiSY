package eu.kanade.presentation.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.presentation.core.util.plus

internal data class FeedItemUI(
    val feed: FeedSavedSearch,
    val savedSearch: SavedSearch?,
    val source: Source?,
    val title: String,
    val subtitle: String,
    val results: List<Manga>?,
)
