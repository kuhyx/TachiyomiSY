package exh.recs.batch

import androidx.compose.runtime.getValue

internal data class RecommendationSearchProgressProperties(
    val title: String,
    val text: String,
    val positiveButtonText: String? = null,
    val positiveButton: (() -> Unit)? = null,
    val negativeButtonText: String? = null,
    val negativeButton: (() -> Unit)? = null,
)
