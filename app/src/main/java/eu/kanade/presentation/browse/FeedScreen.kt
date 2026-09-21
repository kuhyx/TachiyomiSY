package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.feed.FeedScreenState
import kotlinx.coroutines.delay
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun FeedScreen(
    state: FeedScreenState,
    contentPadding: PaddingValues,
    onClickSavedSearch: (SavedSearch, Source) -> Unit,
    onClickSource: (Source) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
    onRefresh: () -> Unit,
    getMangaState: @Composable (Manga) -> State<Manga>,
) {
    when {
        state.isLoading -> {
            LoadingScreen()
        }
        state.isEmpty -> {
            EmptyScreen(
                SYMR.strings.feed_tab_empty,
                modifier = Modifier.padding(contentPadding),
            )
        }
        else -> {
            var refreshing by remember { mutableStateOf(false) }
            LaunchedEffect(refreshing) {
                if (refreshing) {
                    delay(1.seconds)
                    refreshing = false
                }
            }
            PullRefresh(
                refreshing = refreshing && state.isLoadingItems,
                onRefresh = {
                    refreshing = true
                    onRefresh()
                },
                enabled = !state.isLoadingItems,
            ) {
                FeedList(
                    state,
                    contentPadding,
                    onClickSavedSearch,
                    onClickSource,
                    onClickDelete,
                    onClickManga,
                    getMangaState,
                )
            }
        }
    }
}

@Composable
private fun FeedList(
    state: FeedScreenState,
    contentPadding: PaddingValues,
    onClickSavedSearch: (SavedSearch, Source) -> Unit,
    onClickSource: (Source) -> Unit,
    onClickDelete: (FeedSavedSearch) -> Unit,
    onClickManga: (Manga) -> Unit,
    getMangaState: @Composable (Manga) -> State<Manga>,
) {
    ScrollbarLazyColumn(
        contentPadding = contentPadding + topSmallPaddingValues,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            state.items.orEmpty(),
            key = { it.feed.id },
        ) { item ->
            GlobalSearchResultItem(
                title = item.title,
                subtitle = item.subtitle,
                onLongClick = { onClickDelete(item.feed) },
                onClick = {
                    if (item.savedSearch != null && item.source != null) {
                        onClickSavedSearch(item.savedSearch, item.source)
                    } else if (item.source != null) {
                        onClickSource(item.source)
                    }
                },
                modifier = Modifier.animateItem(),
            ) {
                FeedItem(
                    item = item,
                    getMangaState = { getMangaState(it) },
                    onClickManga = onClickManga,
                )
            }
        }
    }
}

@Composable
internal fun FeedItem(
    item: FeedItemUI,
    onClickManga: (Manga) -> Unit,
    getMangaState: @Composable ((Manga) -> State<Manga>),
) {
    when {
        item.results == null -> {
            GlobalSearchLoadingResultItem()
        }
        item.results.isEmpty() -> {
            GlobalSearchErrorResultItem(message = stringResource(MR.strings.no_results_found))
        }
        else -> {
            GlobalSearchCardRow(
                titles = item.results,
                getManga = getMangaState,
                onClick = onClickManga,
                onLongClick = onClickManga,
            )
        }
    }
}
