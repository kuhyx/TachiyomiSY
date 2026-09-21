package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.api.get

// The View-based form; [onInflated] hands the binding back so the dialog buttons can read it.
@Composable
internal fun EditMangaForm(
    manga: Manga,
    scope: CoroutineScope,
    getTracks: GetTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<Track, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
    onInflated: (EditMangaDialogBinding) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        AndroidView(
            factory = { factoryContext ->
                EditMangaDialogBinding.inflate(LayoutInflater.from(factoryContext))
                    .also(onInflated)
                    .apply {
                        onViewCreated(
                            manga,
                            factoryContext,
                            this,
                            scope,
                            getTracks,
                            trackerManager,
                            tracks,
                            showTrackerSelectionDialogue,
                        )
                    }
                    .root
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun EditMangaDialogBinding.submit(
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
    onPositiveClick(
        title.text.toString(),
        mangaAuthor.text.toString(),
        mangaArtist.text.toString(),
        thumbnailUrl.text.toString(),
        mangaDescription.text.toString(),
        mangaGenresTags.getTextStrings(),
        STATUS_OPTIONS.getOrNull(status.selectedItemPosition)?.toLong(),
    )
}

internal fun onViewCreated(
    manga: Manga,
    context: Context,
    binding: EditMangaDialogBinding,
    scope: CoroutineScope,
    getTracks: GetTracks,
    trackerManager: TrackerManager,
    tracks: MutableState<List<Pair<Track, Tracker>>>,
    showTrackerSelectionDialogue: MutableState<Boolean>,
) {
    loadCover(manga, binding)
    binding.setUpStatusSpinner(manga, context)
    if (manga.isLocal()) {
        binding.fillLocalFields(manga, context, scope)
    } else {
        binding.fillSourcedFields(manga, context, scope)
    }
    binding.mangaGenresTags.clearFocus()

    binding.resetTags.setOnClickListener { resetTags(manga, binding, scope) }
    binding.resetInfo.setOnClickListener { resetInfo(manga, binding, scope) }
    binding.autofillFromTracker.setOnClickListener {
        scope.launch {
            getTrackers(manga, binding, context, getTracks, trackerManager, tracks, showTrackerSelectionDialogue)
        }
    }
}

internal fun resetTags(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
        binding.mangaGenresTags.setChips(emptyList(), scope)
    } else {
        binding.mangaGenresTags.setChips(manga.ogGenre.orEmpty(), scope)
    }
}

internal fun loadCover(manga: Manga, binding: EditMangaDialogBinding) {
    binding.mangaCover.load(manga) {
        transformations(RoundedCornersTransformation(4.dpToPx.toFloat()))
    }
}

internal fun resetInfo(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    binding.title.setText("")
    binding.mangaAuthor.setText("")
    binding.mangaArtist.setText("")
    binding.thumbnailUrl.setText("")
    binding.mangaDescription.setText("")
    resetTags(manga, binding, scope)
}
