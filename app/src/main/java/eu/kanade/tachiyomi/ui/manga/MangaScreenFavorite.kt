package eu.kanade.tachiyomi.ui.manga

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.toggleFavorite() {
    toggleFavorite(
        onRemoved = {
            screenModelScope.launch {
                if (downloads.hasDownloads()) {
                    val result = snackbarHostState.showSnackbar(
                        message = context.stringResource(MR.strings.delete_downloads_for_manga),
                        actionLabel = context.stringResource(MR.strings.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        downloads.deleteDownloads()
                    }
                }
            }
        },
    )
}

/**
 * Update favorite status of manga, (removes / adds) manga (to / from) library.
 */
internal fun MangaScreenModel.toggleFavorite(onRemoved: () -> Unit, checkDuplicate: Boolean = true) {
    library.toggleFavorite(onRemoved, checkDuplicate)
}
