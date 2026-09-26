package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.data.Database
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryManga
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ClearDatabaseScreenModel : StateScreenModel<ClearDatabaseScreenModel.State>(State.Loading) {
    private val getSourcesWithNonLibraryManga: GetSourcesWithNonLibraryManga = Injekt.get()
    private val database: Database = Injekt.get()

    init {
        screenModelScope.launchIO {
            getSourcesWithNonLibraryManga.subscribe()
                .collectLatest { list ->
                    mutableState.update { old ->
                        val items = list.sortedBy { it.name }
                        // Loading is the only other state; a sealed `when` would add a dead "no match" arm.
                        if (old is State.Ready) old.copy(items = items) else State.Ready(items)
                    }
                }
        }
    }

    suspend fun removeMangaBySourceId(keepReadManga: Boolean) = withNonCancellableContext {
        val state = state.value as? State.Ready
        if (state != null) {
            database.mangasQueries.deleteNonLibraryManga(state.selection, keepReadManga.toLong())
            database.historyQueries.removeResettedHistory()
        }
    }

    fun toggleSelection(source: Source) = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            val mutableList = state.selection.toMutableList()
            if (mutableList.contains(source.id)) {
                mutableList.remove(source.id)
            } else {
                mutableList.add(source.id)
            }
            state.copy(selection = mutableList)
        }
    }

    fun clearSelection() = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            state.copy(selection = emptyList())
        }
    }

    fun selectAll() = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            state.copy(selection = state.items.fastMap { it.id })
        }
    }

    fun invertSelection() = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            state.copy(
                selection = state.items
                    .fastMap { it.id }
                    .filterNot { it in state.selection },
            )
        }
    }

    fun showConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            state.copy(showConfirmation = true)
        }
    }

    fun hideConfirmation() = mutableState.update { state ->
        if (state !is State.Ready) {
            state
        } else {
            state.copy(showConfirmation = false)
        }
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val items: List<SourceWithCount>,
            val selection: List<Long> = emptyList(),
            val showConfirmation: Boolean = false,
        ) : State
    }
}
