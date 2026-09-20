@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import java.io.Serializable

internal interface Track : Serializable {

    var id: Long?

    var mangaId: Long

    var trackerId: Long

    var remoteId: Long

    var libraryId: Long?

    var title: String

    var lastChapterRead: Double

    var totalChapters: Long

    var score: Double

    var status: Long

    var startedReadingDate: Long

    var finishedReadingDate: Long

    var trackingUrl: String

    var private: Boolean

    fun copyPersonalFrom(other: Track, copyRemotePrivate: Boolean = true) {
        lastChapterRead = other.lastChapterRead
        score = other.score
        status = other.status
        startedReadingDate = other.startedReadingDate
        finishedReadingDate = other.finishedReadingDate
        if (copyRemotePrivate) private = other.private
    }

    companion object {
        fun create(serviceId: Long): Track = TrackImpl().apply {
            trackerId = serviceId
        }
    }
}
