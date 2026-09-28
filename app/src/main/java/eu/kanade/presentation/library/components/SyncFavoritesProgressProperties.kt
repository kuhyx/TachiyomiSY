package eu.kanade.presentation.library.components

import androidx.compose.runtime.getValue
import eu.kanade.presentation.components.DialogButton

internal data class SyncFavoritesProgressProperties(
    val title: String,
    val text: String,
    val positiveButton: DialogButton? = null,
    val negativeButton: DialogButton? = null,
)
