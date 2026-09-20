package eu.kanade.presentation.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

// Latest and Browse have no saved search behind them; they take reserved negative ids.
private const val BROWSE_FEED_ID = -2L

internal sealed class SourceFeedUI {
    abstract val id: Long

    abstract val title: String
        @Composable
        @ReadOnlyComposable
        get

    abstract val results: List<Manga>?

    abstract fun withResults(results: List<Manga>?): SourceFeedUI

    data class Latest(override val results: List<Manga>?) : SourceFeedUI() {
        override val id: Long = -1
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = stringResource(MR.strings.latest)

        override fun withResults(results: List<Manga>?): SourceFeedUI = copy(results = results)
    }
    data class Browse(override val results: List<Manga>?) : SourceFeedUI() {
        override val id: Long = BROWSE_FEED_ID
        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = stringResource(MR.strings.browse)

        override fun withResults(results: List<Manga>?): SourceFeedUI = copy(results = results)
    }
    data class SourceSavedSearch(
        val feed: FeedSavedSearch,
        val savedSearch: SavedSearch,
        override val results: List<Manga>?,
    ) : SourceFeedUI() {
        override val id: Long
            get() = feed.id

        override val title: String
            @Composable
            @ReadOnlyComposable
            get() = savedSearch.name

        override fun withResults(results: List<Manga>?): SourceFeedUI = copy(results = results)
    }
}
