package tachiyomi.domain.track.model

/** A track row of manga [mangaId] on tracker [trackerId] with status [status]. */
internal fun track(id: Long = 1L, mangaId: Long = 10L, trackerId: Long = 1L, status: Long = 1L): Track = Track(
    id = id,
    mangaId = mangaId,
    trackerId = trackerId,
    remoteId = 100L,
    libraryId = null,
    title = "Title",
    lastChapterRead = 12.5,
    totalChapters = 20L,
    status = status,
    score = 8.0,
    remoteUrl = "https://tracker.example/100",
    startDate = 0L,
    finishDate = 0L,
    private = false,
)
