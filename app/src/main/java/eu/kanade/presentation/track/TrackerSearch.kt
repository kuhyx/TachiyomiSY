package eu.kanade.presentation.track

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.util.plus

@Composable
internal fun TrackerSearch(
    state: TextFieldState,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<TrackSearch>>?,
    selected: TrackSearch?,
    onSelectedChange: (TrackSearch) -> Unit,
    onConfirmSelection: (private: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    supportsPrivateTracking: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val dispatchQueryAndClearFocus: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TrackerSearchTopBar(
                state = state,
                focusRequester = focusRequester,
                onDispatchQuery = dispatchQueryAndClearFocus,
                onDismissRequest = onDismissRequest,
            )
        },
        bottomBar = {
            TrackerSearchBottomBar(
                visible = selected != null,
                supportsPrivateTracking = supportsPrivateTracking,
                onConfirmSelection = onConfirmSelection,
            )
        },
    ) { innerPadding ->
        TrackerSearchResults(
            queryResult = queryResult,
            selected = selected,
            onSelectedChange = onSelectedChange,
            innerPadding = innerPadding,
        )
    }
}

@PreviewLightDark
@Composable
internal fun TrackerSearchPreviews(
    @PreviewParameter(TrackerSearchPreviewProvider::class)
    content: @Composable () -> Unit,
) {
    TachiyomiPreviewTheme { content() }
}
