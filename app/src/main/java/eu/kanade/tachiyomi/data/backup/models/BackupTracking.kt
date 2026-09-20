package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.track.model.Track

private const val BACKUP_TRACKING_SYNC_ID = 1
private const val BACKUP_TRACKING_LIBRARY_ID = 2
private const val BACKUP_TRACKING_MEDIA_ID_INT = 3
private const val BACKUP_TRACKING_TRACKING_URL = 4
private const val BACKUP_TRACKING_TITLE = 5
private const val BACKUP_TRACKING_LAST_CHAPTER_READ = 6
private const val BACKUP_TRACKING_TOTAL_CHAPTERS = 7
private const val BACKUP_TRACKING_SCORE = 8
private const val BACKUP_TRACKING_STATUS = 9
private const val BACKUP_TRACKING_STARTED_READING_DATE = 10
private const val BACKUP_TRACKING_FINISHED_READING_DATE = 11
private const val BACKUP_TRACKING_PRIVATE = 12
private const val BACKUP_TRACKING_MEDIA_ID = 100

@Serializable
internal data class BackupTracking(
    // in 1.x some of these values have different types or names
    @ProtoNumber(BACKUP_TRACKING_SYNC_ID) var syncId: Int,
    // LibraryId is not null in 1.x
    @ProtoNumber(BACKUP_TRACKING_LIBRARY_ID) var libraryId: Long,
    @Deprecated("Use mediaId instead", level = DeprecationLevel.WARNING)
    @ProtoNumber(BACKUP_TRACKING_MEDIA_ID_INT)
    var mediaIdInt: Int = 0,
    // trackingUrl is called mediaUrl in 1.x
    @ProtoNumber(BACKUP_TRACKING_TRACKING_URL) var trackingUrl: String = "",
    @ProtoNumber(BACKUP_TRACKING_TITLE) var title: String = "",
    // lastChapterRead is called last read, and it has been changed to a float in 1.x
    @ProtoNumber(BACKUP_TRACKING_LAST_CHAPTER_READ) var lastChapterRead: Float = 0F,
    @ProtoNumber(BACKUP_TRACKING_TOTAL_CHAPTERS) var totalChapters: Int = 0,
    @ProtoNumber(BACKUP_TRACKING_SCORE) var score: Float = 0F,
    @ProtoNumber(BACKUP_TRACKING_STATUS) var status: Int = 0,
    // startedReadingDate is called startReadTime in 1.x
    @ProtoNumber(BACKUP_TRACKING_STARTED_READING_DATE) var startedReadingDate: Long = 0,
    // finishedReadingDate is called endReadTime in 1.x
    @ProtoNumber(BACKUP_TRACKING_FINISHED_READING_DATE) var finishedReadingDate: Long = 0,
    @ProtoNumber(BACKUP_TRACKING_PRIVATE) var private: Boolean = false,
    @ProtoNumber(BACKUP_TRACKING_MEDIA_ID) var mediaId: Long = 0,
) {

    @Suppress("DEPRECATION")
    fun getTrackImpl(): Track {
        return Track(
            id = -1,
            mangaId = -1,
            trackerId = this@BackupTracking.syncId.toLong(),
            remoteId = if (this@BackupTracking.mediaIdInt != 0) {
                this@BackupTracking.mediaIdInt.toLong()
            } else {
                this@BackupTracking.mediaId
            },
            libraryId = this@BackupTracking.libraryId,
            title = this@BackupTracking.title,
            lastChapterRead = this@BackupTracking.lastChapterRead.toDouble(),
            totalChapters = this@BackupTracking.totalChapters.toLong(),
            score = this@BackupTracking.score.toDouble(),
            status = this@BackupTracking.status.toLong(),
            startDate = this@BackupTracking.startedReadingDate,
            finishDate = this@BackupTracking.finishedReadingDate,
            remoteUrl = this@BackupTracking.trackingUrl,
            private = this@BackupTracking.private,
        )
    }
}

internal val backupTrackMapper = {
        _: Long,
        _: Long,
        syncId: Long,
        mediaId: Long,
        libraryId: Long?,
        title: String,
        lastChapterRead: Double,
        totalChapters: Long,
        status: Long,
        score: Double,
        remoteUrl: String,
        startDate: Long,
        finishDate: Long,
        private: Boolean,
    ->
    BackupTracking(
        syncId = syncId.toInt(),
        mediaId = mediaId,
        // forced not null so its compatible with 1.x backup system
        libraryId = libraryId ?: 0,
        title = title,
        lastChapterRead = lastChapterRead.toFloat(),
        totalChapters = totalChapters.toInt(),
        score = score.toFloat(),
        status = status.toInt(),
        startedReadingDate = startDate,
        finishedReadingDate = finishDate,
        trackingUrl = remoteUrl,
        private = private,
    )
}
