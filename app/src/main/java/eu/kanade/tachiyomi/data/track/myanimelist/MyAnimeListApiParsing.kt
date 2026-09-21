package eu.kanade.tachiyomi.data.track.myanimelist

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi.Companion.MANGA_API_URL
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListItemStatus
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALManga
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALMangaMetadata
import eu.kanade.tachiyomi.data.track.myanimelist.dto.getFullName
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import tachiyomi.core.common.util.lang.withIOContext
import java.text.SimpleDateFormat
import java.util.Locale
import tachiyomi.domain.track.model.Track as DomainTrack

internal suspend fun MyAnimeListApi.getMangaMetadata(track: DomainTrack): TrackMangaMetadata? {
    return withIOContext {
        val url = MANGA_API_URL.toUri().buildUpon()
            .appendPath(track.remoteId.toString())
            .appendQueryParameter(
                FIELDS,
                "id,title,synopsis,main_picture,authors{first_name,last_name}",
            )
            .build()
        with(json) {
            authClient.newCall(GET(url.toString()))
                .awaitSuccess()
                .parseAs<MALMangaMetadata>()
                .let {
                    TrackMangaMetadata(
                        remoteId = it.id,
                        title = it.title,
                        thumbnailUrl = it.covers.large.ifEmpty { null } ?: it.covers.medium,
                        description = it.synopsis,
                        authors = it.authors
                            .filter { it.role == "Story" || it.role == "Story & Art" }
                            .mapNotNull { it.node.getFullName() }
                            .joinToString(separator = ", ")
                            .ifEmpty { null },
                        artists = it.authors
                            .filter { it.role == "Art" || it.role == "Story & Art" }
                            .mapNotNull { it.node.getFullName() }
                            .joinToString(separator = ", ")
                            .ifEmpty { null },
                    )
                }
        }
    }
}

internal fun MyAnimeListApi.parseMangaItem(listStatus: MALListItemStatus, track: Track): Track {
    return track.apply {
        val isRereading = listStatus.isRereading
        status = if (isRereading) MyAnimeList.REREADING else getStatus(listStatus.status)
        lastChapterRead = listStatus.numChaptersRead
        score = listStatus.score.toDouble()
        listStatus.startDate?.let { startedReadingDate = parseDate(it) }
        listStatus.finishDate?.let { finishedReadingDate = parseDate(it) }
    }
}

internal fun MyAnimeListApi.parseSearchItem(searchItem: MALManga): TrackSearch {
    return TrackSearch.create(trackId).apply {
        remoteId = searchItem.id
        title = searchItem.title
        summary = searchItem.synopsis
        totalChapters = searchItem.numChapters
        score = searchItem.mean
        coverUrl = searchItem.covers?.large.orEmpty()
        trackingUrl = "https://myanimelist.net/manga/$remoteId"
        publishingStatus = searchItem.status.replace("_", " ")
        publishingType = searchItem.mediaType.replace("_", " ")
        startDate = searchItem.startDate ?: ""
        artists = searchItem.authors
            .filter { authorNode -> authorNode.role == "Art" }
            .mapNotNull { authorNode -> authorNode.node.getFullName() }
        authors = searchItem.authors
            // count all with "Story" or "Story & Art" as authors, like is done for library entries
            .filter { authorNode -> authorNode.role.contains("Story") }
            .mapNotNull { authorNode -> authorNode.node.getFullName() }
    }
}

internal fun MyAnimeListApi.parseDate(
    isoDate: String,
): Long = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)!!.time

internal fun MyAnimeListApi.convertToIsoDate(epochTime: Long): String {
    if (epochTime == 0L) {
        return ""
    }
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(epochTime)
}
