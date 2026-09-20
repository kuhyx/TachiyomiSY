@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

internal class TrackImpl : Track {

    override var id: Long? = null

    override var mangaId: Long = 0

    override var trackerId: Long = 0

    override var remoteId: Long = 0

    override var libraryId: Long? = null

    override lateinit var title: String

    override var lastChapterRead: Double = 0.0

    override var totalChapters: Long = 0

    override var score: Double = 0.0

    override var status: Long = 0

    override var startedReadingDate: Long = 0

    override var finishedReadingDate: Long = 0

    override var trackingUrl: String = ""

    override var private: Boolean = false
}
