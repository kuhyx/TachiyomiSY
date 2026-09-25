package eu.kanade.presentation.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch

/** A search result with every detail row filled; blank the fields a test needs empty. */
internal fun trackSearch(
    title: String,
    summary: String = "  A summary.  ",
    score: Double = 7.5,
    trackingUrl: String = "https://example.com/t",
): TrackSearch = TrackSearch().also {
    it.title = title
    it.summary = summary
    it.score = score
    it.trackingUrl = trackingUrl
    it.coverUrl = ""
    it.startDate = "2020-01-01"
    it.publishingStatus = "FINISHED"
    it.publishingType = "MANGA"
    it.authors = listOf("Author")
    it.artists = listOf("Author", "Artist")
}
