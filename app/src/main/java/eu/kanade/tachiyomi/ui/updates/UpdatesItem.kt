package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import eu.kanade.core.util.insertSeparators
import eu.kanade.presentation.updates.UpdatesUiModel
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.lang.toLocalDate
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.model.UpdatesWithRelations
import uy.kohesive.injekt.api.get

internal fun TriState.toBooleanOrNull(): Boolean? {
    return when (this) {
        TriState.DISABLED -> null
        TriState.ENABLED_IS -> true
        TriState.ENABLED_NOT -> false
    }
}

@Immutable
internal data class UpdatesItem(
    val update: UpdatesWithRelations,
    val downloadStateProvider: () -> Download.State,
    val downloadProgressProvider: () -> Int,
    val selected: Boolean = false,
) {
    // SY -->
    // SY <--
}

internal fun UpdatesItem.isEhBasedUpdate(): Boolean =
    update.sourceId == EH_SOURCE_ID || update.sourceId == EXH_SOURCE_ID

internal fun UpdatesScreenModel.State.getUiModel(): List<UpdatesUiModel> {
    return items
        .map { UpdatesUiModel.Item(it) }
        .insertSeparators { before, after ->
            val beforeDate = before?.item?.update?.dateFetch?.toLocalDate()
            val afterDate = after?.item?.update?.dateFetch?.toLocalDate()
            if (beforeDate != afterDate && afterDate != null) {
                UpdatesUiModel.Header(afterDate)
            } else {
                null
            }
        }
}
