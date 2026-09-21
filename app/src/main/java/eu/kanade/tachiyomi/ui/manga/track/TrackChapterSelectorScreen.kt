package eu.kanade.tachiyomi.ui.manga.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.presentation.track.TrackChapterSelector
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val MAX_CHAPTERS = 10_000

internal data class TrackChapterSelectorScreen(
    private val track: Track,
    private val serviceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            Model(
                track = track,
                tracker = Injekt.get<TrackerManager>().get(serviceId)!!,
            )
        }
        val state by screenModel.state.collectAsState()

        TrackChapterSelector(
            selection = state.selection,
            onSelectionChange = screenModel::setSelection,
            range = remember { screenModel.getRange() },
            onConfirm = {
                screenModel.setChapter()
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
        )
    }

    private class Model(
        private val track: Track,
        private val tracker: Tracker,
    ) : StateScreenModel<Model.State>(State(track.lastChapterRead.toInt())) {

        fun getRange(): Iterable<Int> {
            val endRange = if (track.totalChapters > 0) {
                track.totalChapters
            } else {
                MAX_CHAPTERS
            }
            return 0..endRange.toInt()
        }

        fun setSelection(selection: Int) {
            mutableState.update { it.copy(selection = selection) }
        }

        fun setChapter() {
            screenModelScope.launchNonCancellable {
                tracker.setRemoteLastChapterRead(track.toDbTrack(), state.value.selection)
            }
        }

        @Immutable
        data class State(
            val selection: Int,
        )
    }
}
