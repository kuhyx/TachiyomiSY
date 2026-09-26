package exh.recs.batch

import androidx.compose.runtime.getValue

internal data class RecommendationSearchProgressProperties(
    val title: String,
    val text: String,
    val positiveButton: ProgressDialogButton? = null,
    val negativeButton: ProgressDialogButton? = null,
)

/** A dialog button's label and action, which only ever exist together. */
internal data class ProgressDialogButton(val text: String, val onClick: () -> Unit)
