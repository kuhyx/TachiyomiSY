package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.widget.ArrayAdapter
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.chop
import exh.util.dropBlank
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

// How the edit dialog's fields are pre-filled from the manga being edited.

// Spinner positions of the status picker; the first entry is "default" (keep the source's status).
internal val STATUS_OPTIONS = listOf(
    null,
    SManga.ONGOING,
    SManga.COMPLETED,
    SManga.LICENSED,
    SManga.PUBLISHING_FINISHED,
    SManga.CANCELLED,
    SManga.ON_HIATUS,
)

// Status codes older SY builds stored for the three statuses Mihon later numbered.
private const val LEGACY_PUBLISHING_FINISHED = 61
private const val LEGACY_CANCELLED = 62
private const val LEGACY_ON_HIATUS = 63

private const val DESCRIPTION_HINT_LENGTH = 20
private const val THUMBNAIL_HINT_LENGTH = 40
private const val EXTENSION_HINT_LENGTH = 6

internal fun EditMangaDialogBinding.setUpStatusSpinner(manga: Manga, context: Context) {
    val statusAdapter: ArrayAdapter<String> = ArrayAdapter(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        listOf(
            MR.strings.label_default,
            MR.strings.ongoing,
            MR.strings.completed,
            MR.strings.licensed,
            MR.strings.publishing_finished,
            MR.strings.cancelled,
            MR.strings.on_hiatus,
        ).map { context.stringResource(it) },
    )
    status.adapter = statusAdapter
    if (manga.status != manga.ogStatus) {
        val statusValue = when (manga.status.toInt()) {
            LEGACY_PUBLISHING_FINISHED -> SManga.PUBLISHING_FINISHED
            LEGACY_CANCELLED -> SManga.CANCELLED
            LEGACY_ON_HIATUS -> SManga.ON_HIATUS
            else -> manga.status.toInt()
        }
        status.setSelection(STATUS_OPTIONS.indexOf(statusValue).coerceAtLeast(0))
    }
}

/** A local entry has no source copy to fall back on, so every field shows its current value. */
internal fun EditMangaDialogBinding.fillLocalFields(manga: Manga, context: Context, scope: CoroutineScope) {
    if (manga.title != manga.url) {
        title.setText(manga.title)
    }
    title.hint = context.stringResource(SYMR.strings.title_hint, manga.url)
    mangaAuthor.setText(manga.author.orEmpty())
    mangaArtist.setText(manga.artist.orEmpty())
    thumbnailUrl.setText(manga.thumbnailUrl.orEmpty())
    mangaDescription.setText(manga.description.orEmpty())
    mangaGenresTags.setChips(manga.genre.orEmpty().dropBlank(), scope)
}

/** A sourced entry shows only its overrides as text; the source values sit in the hints. */
internal fun EditMangaDialogBinding.fillSourcedFields(manga: Manga, context: Context, scope: CoroutineScope) {
    if (manga.title != manga.ogTitle) {
        title.append(manga.title)
    }
    if (manga.author != manga.ogAuthor) {
        mangaAuthor.append(manga.author.orEmpty())
    }
    if (manga.artist != manga.ogArtist) {
        mangaArtist.append(manga.artist.orEmpty())
    }
    if (manga.thumbnailUrl != manga.ogThumbnailUrl) {
        thumbnailUrl.append(manga.thumbnailUrl.orEmpty())
    }
    if (manga.description != manga.ogDescription) {
        mangaDescription.append(manga.description.orEmpty())
    }
    mangaGenresTags.setChips(manga.genre.orEmpty().dropBlank(), scope)

    title.hint = context.stringResource(SYMR.strings.title_hint, manga.ogTitle)
    mangaAuthor.hint = context.stringResource(SYMR.strings.author_hint, manga.ogAuthor ?: "")
    mangaArtist.hint = context.stringResource(SYMR.strings.artist_hint, manga.ogArtist ?: "")
    mangaDescription.hint = context.stringResource(
        SYMR.strings.description_hint,
        manga.ogDescription
            ?.takeIf { it.isNotBlank() }
            ?.replace("\n", " ")
            ?.chop(DESCRIPTION_HINT_LENGTH)
            ?: "",
    )
    thumbnailUrl.hint =
        context.stringResource(SYMR.strings.thumbnail_url_hint, manga.ogThumbnailUrl?.let(::thumbnailHint) ?: "")
}

// The url shortened to its head plus, when long enough to have lost it, the file extension.
private fun thumbnailHint(url: String): String {
    val extension = if (url.length > THUMBNAIL_HINT_LENGTH + EXTENSION_HINT_LENGTH) {
        "." + url.substringAfterLast(".").chop(EXTENSION_HINT_LENGTH)
    } else {
        ""
    }
    return url.chop(THUMBNAIL_HINT_LENGTH) + extension
}
