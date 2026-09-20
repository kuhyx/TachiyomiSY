package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

@Composable
internal fun TrackerSelectDialog(
    tracks: List<Pair<Track, Tracker>>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (
        tracker: Tracker,
        track: Track,
    ) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(stringResource(SYMR.strings.select_tracker))
        },
        text = {
            FlowRow(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tracks.forEach { (track, tracker) ->
                    TrackLogoIcon(
                        tracker,
                        onClick = {
                            onTrackerSelect(tracker, track)
                            onDismissRequest()
                        },
                    )
                }
            }
        },
    )
}

internal suspend fun getTrackers(
    manga: Manga,
    binding: EditMangaDialogBinding,
    context: Context,
    getTracks: GetTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<Track, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    tracks.value = getTracks.await(manga.id).mapNotNull { track ->
        track to (trackerManager.get(track.trackerId) ?: return@mapNotNull null)
    }
        .filterNot { (_, tracker) -> tracker is EnhancedTracker }

    if (tracks.value.isEmpty()) {
        context.toast(context.stringResource(SYMR.strings.entry_not_tracked))
        return
    }

    if (tracks.value.size > 1) {
        showTrackerSelectionDialogue.value = true
        return
    }

    autofillFromTracker(binding, tracks.value.first().first, tracks.value.first().second)
}

internal suspend fun autofillFromTracker(binding: EditMangaDialogBinding, track: Track, tracker: Tracker) {
    try {
        val trackerMangaMetadata = tracker.getMangaMetadata(track)

        setTextIfNotBlank(binding.title::setText, trackerMangaMetadata?.title)
        setTextIfNotBlank(binding.mangaAuthor::setText, trackerMangaMetadata?.authors)
        setTextIfNotBlank(binding.mangaArtist::setText, trackerMangaMetadata?.artists)
        setTextIfNotBlank(binding.thumbnailUrl::setText, trackerMangaMetadata?.thumbnailUrl)
        setTextIfNotBlank(binding.mangaDescription::setText, trackerMangaMetadata?.description)
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        tracker.logcat(LogPriority.ERROR, expected)
        binding.root.context.toast(
            binding.root.context.stringResource(
                MR.strings.track_error,
                tracker.name,
                expected.message ?: "",
            ),
        )
    }
}

internal fun setTextIfNotBlank(field: (String) -> Unit, value: String?) {
    value?.takeIf { it.isNotBlank() }?.let { field(it) }
}
