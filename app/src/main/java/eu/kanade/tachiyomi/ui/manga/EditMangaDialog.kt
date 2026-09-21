package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.materialdialogs.setTextInput
import exh.ui.metadata.adapters.MetadataUIUtil.getResourceColor
import exh.util.trimOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
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

// The View-based form; [onInflated] hands the binding back so the dialog buttons can read it.
@Composable
private fun EditMangaForm(
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

private fun EditMangaDialogBinding.submit(
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

private fun onViewCreated(
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

private fun resetTags(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
        binding.mangaGenresTags.setChips(emptyList(), scope)
    } else {
        binding.mangaGenresTags.setChips(manga.ogGenre.orEmpty(), scope)
    }
}

private fun loadCover(manga: Manga, binding: EditMangaDialogBinding) {
    binding.mangaCover.load(manga) {
        transformations(RoundedCornersTransformation(4.dpToPx.toFloat()))
    }
}

private fun resetInfo(manga: Manga, binding: EditMangaDialogBinding, scope: CoroutineScope) {
    binding.title.setText("")
    binding.mangaAuthor.setText("")
    binding.mangaArtist.setText("")
    binding.thumbnailUrl.setText("")
    binding.mangaDescription.setText("")
    resetTags(manga, binding, scope)
}

internal fun ChipGroup.setChips(items: List<String>, scope: CoroutineScope) {
    removeAllViews()

    items.asSequence().map { item ->
        Chip(context).apply {
            text = item

            isCloseIconVisible = true
            closeIcon?.setTint(context.getResourceColor(R.attr.colorAccent))
            setOnCloseIconClickListener {
                removeView(this)
            }
        }
    }.forEach {
        addView(it)
    }

    val addTagChip = Chip(context).apply {
        setText(SYMR.strings.add_tags.getString(context))

        chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_add_24dp)?.apply {
            isChipIconVisible = true
            setTint(context.getResourceColor(R.attr.colorAccent))
        }

        setOnClickListener {
            var newTags: String? = null
            MaterialAlertDialogBuilder(context)
                .setTitle(SYMR.strings.add_tags.getString(context))
                .setMessage(SYMR.strings.multi_tags_comma_separated.getString(context))
                .setTextInput { newTags = it.trimOrNull() }
                .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                    newTags?.let {
                        setChips(items + it.split(",").map { it.trimOrNull() }.filterNotNull(), scope)
                    }
                }
                .setNegativeButton(MR.strings.action_cancel.getString(context), null)
                .show()
        }
    }
    addView(addTagChip)
}

private fun ChipGroup.getTextStrings(): List<String> = children.mapNotNull {
    if (it is Chip && !it.text.toString().contains(context.stringResource(SYMR.strings.add_tags), ignoreCase = true)) {
        it.text.toString()
    } else {
        null
    }
}.toList()
