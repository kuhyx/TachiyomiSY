package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackerSearch
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackerSearchScreen(
    private val mangaId: Long,
    private val initialQuery: String,
    private val currentUrl: String?,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                mangaId = mangaId,
                currentUrl = currentUrl,
                initialQuery = initialQuery,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }

        val state by screenModel.state.collectAsState()

        val textFieldState = rememberTextFieldState(initialQuery)
        TrackerSearch(
            state = textFieldState,
            onDispatchQuery = { screenModel.trackingSearch(textFieldState.text.toString()) },
            queryResult = state.queryResult,
            selected = state.selected,
            onSelectedChange = screenModel::updateSelection,
            onConfirmSelection = { private: Boolean ->
                val selected = state.selected
                if (selected != null) {
                    selected.private = private
                    screenModel.registerTracking(selected)
                    navigator.pop()
                }
            },
            onDismissRequest = navigator::pop,
            supportsPrivateTracking = screenModel.supportsPrivateTracking,
        )
    }

    private class Model(
        private val mangaId: Long,
        private val currentUrl: String? = null,
        initialQuery: String,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State()) {

        val supportsPrivateTracking = tracker.supportsPrivateTracking

        init {
            // Run search on first launch
            if (initialQuery.isNotBlank()) {
                trackingSearch(initialQuery)
            }
        }

        fun trackingSearch(query: String) {
            screenModelScope.launch {
                // To show loading state
                mutableState.update { it.copy(queryResult = null, selected = null) }

                val result = withIOContext {
                    try {
                        val results = tracker.search(query)
                        Result.success(results)
                    } catch (expected: Throwable) {
                        // Any failure ends here and the fallback below applies.
                        Result.failure(expected)
                    }
                }
                mutableState.update { oldState ->
                    oldState.copy(
                        queryResult = result,
                        selected = result.getOrNull()?.find { it.trackingUrl == currentUrl },
                    )
                }
            }
        }

        fun registerTracking(item: TrackSearch) {
            screenModelScope.launchNonCancellable { tracker.register(item, mangaId) }
        }

        fun updateSelection(selected: TrackSearch) {
            mutableState.update { it.copy(selected = selected) }
        }

        @Immutable
        data class State(
            val queryResult: Result<List<TrackSearch>>? = null,
            val selected: TrackSearch? = null,
        )
    }
}
