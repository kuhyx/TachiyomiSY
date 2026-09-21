package eu.kanade.tachiyomi.ui.browse.source.browse

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.browse.components.SavedSearchCreateDialog
import eu.kanade.presentation.browse.components.SavedSearchDeleteDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import exh.md.follows.MangaDexFollowsScreen
import mihon.feature.migration.dialog.MigrateMangaDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

// The sections of [BrowseSourceScreen]: the toolbar with its listing chips, and the dialog switch.

@Composable
internal fun BrowseSourceTopBar(
    screenModel: BrowseSourceScreenModel,
    state: BrowseSourceScreenModel.State,
    navigator: Navigator,
    navigateUp: () -> Unit,
    onWebViewClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {},
    ) {
        BrowseSourceToolbar(
            searchQuery = state.toolbarQuery,
            onSearchQueryChange = screenModel::setToolbarQuery,
            source = screenModel.source,
            displayMode = screenModel.displayMode,
            onDisplayModeChange = { screenModel.displayMode = it },
            navigateUp = navigateUp,
            onWebViewClick = onWebViewClick,
            onHelpClick = onHelpClick,
            onSettingsClick = { navigator.push(SourcePreferencesScreen(screenModel.sourceId)) },
            onSearch = screenModel::search,
        )
        ListingChips(screenModel, state)
        HorizontalDivider()
    }
}

@Composable
private fun ListingChips(screenModel: BrowseSourceScreenModel, state: BrowseSourceScreenModel.State) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        ListingChip(
            selected = state.listing == Listing.Popular,
            onClick = {
                screenModel.resetFilters()
                screenModel.setListing(Listing.Popular)
            },
            icon = Icons.Outlined.Favorite,
            label = stringResource(MR.strings.popular),
        )
        if (screenModel.source.supportsLatest) {
            ListingChip(
                selected = state.listing == Listing.Latest,
                onClick = {
                    screenModel.resetFilters()
                    screenModel.setListing(Listing.Latest)
                },
                icon = Icons.Outlined.NewReleases,
                label = stringResource(MR.strings.latest),
            )
        }
        if (/* SY --> */ state.filterable /* SY <-- */) {
            ListingChip(
                selected = state.listing is Listing.Search,
                onClick = screenModel::openFilterSheet,
                icon = Icons.Outlined.FilterList,
                // SY -->
                label = if (state.filters.isNotEmpty()) {
                    stringResource(MR.strings.action_filter)
                } else {
                    stringResource(MR.strings.action_search)
                },
                // SY <--
            )
        }
    }
}

@Composable
private fun ListingChip(selected: Boolean, onClick: () -> Unit, icon: ImageVector, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        label = { Text(text = label) },
    )
}

@Composable
internal fun BrowseSourceScreen.BrowseSourceDialogs(
    screenModel: BrowseSourceScreenModel,
    state: BrowseSourceScreenModel.State,
    navigator: Navigator,
    context: Context,
) {
    val onDismissRequest = { screenModel.setDialog(null) }
    when (val dialog = state.dialog) {
        is BrowseSourceScreenModel.Dialog.Filter -> {
            BrowseSourceFilterDialog(screenModel, state, navigator, context, onDismissRequest)
        }
        is BrowseSourceScreenModel.Dialog.AddDuplicateManga -> {
            DuplicateMangaDialog(
                duplicates = dialog.duplicates,
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.addFavorite(dialog.manga) },
                onOpenManga = { navigator.push(MangaScreen(it.id)) },
                onMigrate = { screenModel.setDialog(BrowseSourceScreenModel.Dialog.Migrate(dialog.manga, it)) },
            )
        }
        is BrowseSourceScreenModel.Dialog.Migrate -> {
            MigrateMangaDialog(
                current = dialog.current,
                target = dialog.target,
                // Initiated from the context of [dialog.target] so we show [dialog.current].
                onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                onDismissRequest = onDismissRequest,
            )
        }
        is BrowseSourceScreenModel.Dialog.RemoveManga -> {
            RemoveMangaDialog(
                onDismissRequest = onDismissRequest,
                onConfirm = { screenModel.changeMangaFavorite(dialog.manga) },
                mangaToRemove = dialog.manga,
            )
        }
        is BrowseSourceScreenModel.Dialog.ChangeMangaCategory -> {
            ChangeCategoryDialog(
                initialSelection = dialog.initialSelection,
                onDismissRequest = onDismissRequest,
                onEditCategories = { navigator.push(CategoryScreen()) },
                onConfirm = { include, _ ->
                    screenModel.changeMangaFavorite(dialog.manga)
                    screenModel.moveMangaToCategories(dialog.manga, include)
                },
            )
        }
        is BrowseSourceScreenModel.Dialog.CreateSavedSearch -> {
            SavedSearchCreateDialog(
                onDismissRequest = onDismissRequest,
                currentSavedSearches = dialog.currentSavedSearches,
                saveSearch = screenModel::saveSearch,
            )
        }
        is BrowseSourceScreenModel.Dialog.DeleteSavedSearch -> {
            SavedSearchDeleteDialog(
                onDismissRequest = onDismissRequest,
                name = dialog.name,
                deleteSavedSearch = { screenModel.deleteSearch(dialog.idToDelete) },
            )
        }
        else -> {}
    }
}

@Composable
private fun BrowseSourceFilterDialog(
    screenModel: BrowseSourceScreenModel,
    state: BrowseSourceScreenModel.State,
    navigator: Navigator,
    context: Context,
    onDismissRequest: () -> Unit,
) {
    val sourceId = screenModel.sourceId
    SourceFilterDialog(
        onDismissRequest = onDismissRequest,
        filters = state.filters,
        onReset = screenModel::resetFilters,
        onFilter = { screenModel.search(filters = state.filters) },
        onUpdate = screenModel::setFilters,
        // SY -->
        startExpanded = screenModel.startExpanded,
        onSave = screenModel::onSaveSearch,
        savedSearches = state.savedSearches,
        onSavedSearch = { search ->
            screenModel.onSavedSearch(search) {
                context.toast(it)
            }
        },
        onSavedSearchPress = screenModel::onSavedSearchPress,
        openMangaDexRandom = if (screenModel.sourceIsMangaDex) {
            {
                screenModel.onMangaDexRandom {
                    navigator.replace(BrowseSourceScreen(sourceId, "id:$it"))
                }
            }
        } else {
            null
        },
        openMangaDexFollows = if (screenModel.sourceIsMangaDex) {
            { navigator.replace(MangaDexFollowsScreen(sourceId)) }
        } else {
            null
        },
        // SY <--
    )
}
