package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.contains
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus

@Composable
internal fun SourcesScreen(
    state: SourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
) {
    when {
        state.isLoading -> {
            LoadingScreen(Modifier.padding(contentPadding))
        }
        state.isEmpty -> {
            EmptyScreen(
                MR.strings.source_empty_screen,
                modifier = Modifier.padding(contentPadding),
            )
        }
        else -> {
            ScrollbarLazyColumn(
                contentPadding = contentPadding + topSmallPaddingValues,
            ) {
                items(
                    items = state.items,
                    contentType = {
                        when (it) {
                            is SourceUiModel.Header -> "header"
                            is SourceUiModel.Item -> "item"
                        }
                    },
                    key = {
                        when (it) {
                            is SourceUiModel.Header -> it.hashCode()
                            is SourceUiModel.Item -> "source-${it.source.key()}"
                        }
                    },
                ) { model ->
                    when (model) {
                        is SourceUiModel.Header -> {
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                                // SY -->
                                isCategory = model.isCategory,
                                // SY <--
                            )
                        }
                        is SourceUiModel.Item -> {
                            SourceItem(
                                modifier = Modifier.animateItem(),
                                source = model.source,
                                // SY -->
                                showLatest = state.showLatest,
                                showPin = state.showPin,
                                // SY <--
                                onClickItem = onClickItem,
                                onLongClickItem = onLongClickItem,
                                onClickPin = onClickPin,
                            )
                        }
                    }
                }
            }
        }
    }
}

internal sealed interface SourceUiModel {
    data class Item(val source: Source) : SourceUiModel
    data class Header(val language: String, val isCategory: Boolean) : SourceUiModel
}

// SY <--
