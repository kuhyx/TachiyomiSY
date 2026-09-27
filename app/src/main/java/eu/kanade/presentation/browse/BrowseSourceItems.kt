package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGrid
import eu.kanade.presentation.browse.components.BrowseSourceCompactGrid
import eu.kanade.presentation.browse.components.BrowseSourceList
import exh.metadata.metadata.RaisedSearchMetadata
import kotlinx.coroutines.flow.StateFlow
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.Manga

@Composable
internal fun BrowseSourceItems(
    displayMode: LibraryDisplayMode,
    mangaList: LazyPagingItems<StateFlow</* SY --> */Pair<Manga, RaisedSearchMetadata?>/* SY <-- */>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    browseItemsContent(
        displayMode = displayMode,
        mangaList = mangaList,
        columns = columns,
        contentPadding = contentPadding,
        onMangaClick = onMangaClick,
        onMangaLongClick = onMangaLongClick,
    )()
}

// Plain so the exhaustive `when` stays out of Compose.
private fun browseItemsContent(
    displayMode: LibraryDisplayMode,
    mangaList: LazyPagingItems<StateFlow<Pair<Manga, RaisedSearchMetadata?>>>,
    columns: GridCells,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
): @Composable () -> Unit = when (displayMode) {
    LibraryDisplayMode.ComfortableGrid -> {
        {
            BrowseSourceComfortableGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
    LibraryDisplayMode.List -> {
        {
            BrowseSourceList(
                mangaList = mangaList,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
    LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
        {
            BrowseSourceCompactGrid(
                mangaList = mangaList,
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
    }
}
