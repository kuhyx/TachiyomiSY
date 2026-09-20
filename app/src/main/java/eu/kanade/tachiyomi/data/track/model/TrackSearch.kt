@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.track.model

import eu.kanade.tachiyomi.data.database.models.Track

internal class TrackSearch : Track {

    override var id: Long? = null

    override var mangaId: Long = 0

    override var trackerId: Long = 0

    override var remoteId: Long = 0

    override var libraryId: Long? = null

    override var title: String = ""

    override var lastChapterRead: Double = 0.0

    override var totalChapters: Long = 0

    override var score: Double = -1.0

    override var status: Long = 0

    override var startedReadingDate: Long = 0

    override var finishedReadingDate: Long = 0

    override var private: Boolean = false

    override var trackingUrl: String = ""

    var authors: List<String> = emptyList()

    var artists: List<String> = emptyList()

    var coverUrl: String = ""

    var summary: String = ""

    var publishingStatus: String = ""

    var publishingType: String = ""

    var startDate: String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackSearch

        if (mangaId != other.mangaId) return false
        if (trackerId != other.trackerId) return false
        if (remoteId != other.remoteId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mangaId.hashCode()
        result = 31 * result + trackerId.hashCode()
        result = 31 * result + remoteId.hashCode()
        return result
    }

    companion object {
        fun create(serviceId: Long): TrackSearch = TrackSearch().apply {
            trackerId = serviceId
        }
    }
}
