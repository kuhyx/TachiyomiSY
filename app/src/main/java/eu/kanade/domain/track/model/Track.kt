package eu.kanade.domain.track.model

import tachiyomi.domain.track.model.Track
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

internal fun Track.copyPersonalFrom(other: Track): Track {
    return this.copy(
        lastChapterRead = other.lastChapterRead,
        score = other.score,
        status = other.status,
        startDate = other.startDate,
        finishDate = other.finishDate,
        private = other.private,
    )
}

internal fun Track.toDbTrack(): DbTrack = DbTrack.create(trackerId).also {
    it.id = id
    it.mangaId = mangaId
    it.remoteId = remoteId
    it.libraryId = libraryId
    it.title = title
    it.lastChapterRead = lastChapterRead
    it.totalChapters = totalChapters
    it.status = status
    it.score = score
    it.trackingUrl = remoteUrl
    it.startedReadingDate = startDate
    it.finishedReadingDate = finishDate
    it.private = private
}

internal fun DbTrack.toDomainTrack(idRequired: Boolean = true): Track? {
    val trackId = id ?: if (!idRequired) -1 else return null
    return Track(
        id = trackId,
        mangaId = mangaId,
        trackerId = trackerId,
        remoteId = remoteId,
        libraryId = libraryId,
        title = title,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
        status = status,
        score = score,
        remoteUrl = trackingUrl,
        startDate = startedReadingDate,
        finishDate = finishedReadingDate,
        private = private,
    )
}
