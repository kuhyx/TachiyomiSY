package exh.recs.batch

import androidx.compose.runtime.getValue
import eu.kanade.presentation.components.DialogButton

internal data class RecommendationSearchProgressProperties(
    val title: String,
    val text: String,
    val positiveButton: DialogButton? = null,
    val negativeButton: DialogButton? = null,
)
