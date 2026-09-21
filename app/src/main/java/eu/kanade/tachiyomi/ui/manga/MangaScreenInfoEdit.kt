package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.model.toSManga
import exh.util.nullIfEmpty
import exh.util.trimOrNull
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.updateMangaInfo(
    title: String?,
    author: String?,
    artist: String?,
    thumbnailUrl: String?,
    description: String?,
    tags: List<String>?,
    status: Long?,
) {
    val state = successState ?: return
    var manga = state.manga
    if (state.manga.isLocal()) {
        val newTitle = if (title.isNullOrBlank()) manga.url else title.trim()
        val newAuthor = author?.trimOrNull()
        val newArtist = artist?.trimOrNull()
        val newThumbnailUrl = thumbnailUrl?.trimOrNull()
        val newDesc = description?.trimOrNull()
        manga = manga.copy(
            ogTitle = newTitle,
            ogAuthor = author?.trimOrNull(),
            ogArtist = artist?.trimOrNull(),
            ogThumbnailUrl = thumbnailUrl?.trimOrNull(),
            ogDescription = description?.trimOrNull(),
            ogGenre = tags?.nullIfEmpty(),
            ogStatus = status ?: 0,
            lastUpdate = manga.lastUpdate + 1,
        )
        (sourceManager.get(LocalSource.ID) as LocalSource).updateMangaInfo(manga.toSManga())
        screenModelScope.launchNonCancellable {
            updateManga.await(
                MangaUpdate(
                    manga.id,
                    title = newTitle,
                    author = newAuthor,
                    artist = newArtist,
                    thumbnailUrl = newThumbnailUrl,
                    description = newDesc,
                    genre = tags,
                    status = status,
                ),
            )
        }
    } else {
        val genre = if (!tags.isNullOrEmpty() && tags != state.manga.ogGenre) {
            tags
        } else {
            null
        }
        setCustomMangaInfo.set(
            CustomMangaInfo(
                state.manga.id,
                title?.trimOrNull(),
                author?.trimOrNull(),
                artist?.trimOrNull(),
                thumbnailUrl?.trimOrNull(),
                description?.trimOrNull(),
                genre,
                status.takeUnless { it == state.manga.ogStatus },
            ),
        )
        manga = manga.copy(lastUpdate = manga.lastUpdate + 1)
    }

    updateSuccessState { successState ->
        successState.copy(manga = manga)
    }
}
