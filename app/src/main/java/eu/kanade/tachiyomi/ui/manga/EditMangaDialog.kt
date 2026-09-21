package eu.kanade.tachiyomi.ui.manga

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun EditMangaDialog(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onPositiveClick: (
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var binding by remember {
        mutableStateOf<EditMangaDialogBinding?>(null)
    }
    val showTrackerSelectionDialogue = remember { mutableStateOf(false) }
    val getTracks = remember { Injekt.get<GetTracks>() }
    val trackerManager = remember { Injekt.get<TrackerManager>() }
    val tracks = remember { mutableStateOf(emptyList<Pair<Track, Tracker>>()) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    binding?.let {
                        it.submit(onPositiveClick)
                        onDismissRequest()
                    }
                },
            ) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        text = {
            EditMangaForm(
                manga = manga,
                scope = scope,
                getTracks = getTracks,
                trackerManager = trackerManager,
                tracks = tracks,
                showTrackerSelectionDialogue = showTrackerSelectionDialogue,
                onInflated = { binding = it },
            )
        },
    )

    if (showTrackerSelectionDialogue.value) {
        TrackerSelectDialog(
            tracks = tracks.value,
            onDismissRequest = { showTrackerSelectionDialogue.value = false },
            onTrackerSelect = { tracker, track ->
                scope.launch {
                    autofillFromTracker(binding!!, track, tracker)
                }
            },
        )
    }
}
