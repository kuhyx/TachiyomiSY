package eu.kanade.tachiyomi.ui.history

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel.Event
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.history.model.HistoryWithRelations
import uy.kohesive.injekt.api.get

internal fun HistoryScreenModel.removeFromHistory(history: HistoryWithRelations) {
    screenModelScope.launchIO {
        removeHistory.await(history)
    }
}

internal fun HistoryScreenModel.removeAllFromHistory(mangaId: Long) {
    screenModelScope.launchIO {
        removeHistory.await(mangaId)
    }
}

internal fun HistoryScreenModel.removeAllHistory() {
    screenModelScope.launchIO {
        val result = removeHistory.awaitAll()
        if (result) {
            emit(Event.HistoryCleared)
        }
    }
}
