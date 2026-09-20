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
    @ProtoNumber(BACKUP_TRACKING_SYNC_ID) val syncId: Int,
    // LibraryId is not null in 1.x
    @ProtoNumber(BACKUP_TRACKING_LIBRARY_ID) val libraryId: Long,
    @Deprecated("Use mediaId instead", level = DeprecationLevel.WARNING)
    @ProtoNumber(BACKUP_TRACKING_MEDIA_ID_INT)
    val mediaIdInt: Int = 0,
    // trackingUrl is called mediaUrl in 1.x
    @ProtoNumber(BACKUP_TRACKING_TRACKING_URL) val trackingUrl: String = "",
    @ProtoNumber(BACKUP_TRACKING_TITLE) val title: String = "",
    // lastChapterRead is called last read, and it has been changed to a float in 1.x
    @ProtoNumber(BACKUP_TRACKING_LAST_CHAPTER_READ) val lastChapterRead: Float = 0F,
    @ProtoNumber(BACKUP_TRACKING_TOTAL_CHAPTERS) val totalChapters: Int = 0,
    @ProtoNumber(BACKUP_TRACKING_SCORE) val score: Float = 0F,
    @ProtoNumber(BACKUP_TRACKING_STATUS) val status: Int = 0,
    // startedReadingDate is called startReadTime in 1.x
    @ProtoNumber(BACKUP_TRACKING_STARTED_READING_DATE) val startedReadingDate: Long = 0,
    // finishedReadingDate is called endReadTime in 1.x
    @ProtoNumber(BACKUP_TRACKING_FINISHED_READING_DATE) val finishedReadingDate: Long = 0,
    @ProtoNumber(BACKUP_TRACKING_PRIVATE) val private: Boolean = false,
    @ProtoNumber(BACKUP_TRACKING_MEDIA_ID) val mediaId: Long = 0,
)

@Suppress("DEPRECATION")
internal fun BackupTracking.getTrackImpl(): Track {
    return Track(
        id = -1,
        mangaId = -1,
        trackerId = this@getTrackImpl.syncId.toLong(),
        remoteId = if (this@getTrackImpl.mediaIdInt != 0) {
            this@getTrackImpl.mediaIdInt.toLong()
        } else {
            this@getTrackImpl.mediaId
        },
        libraryId = this@getTrackImpl.libraryId,
        title = this@getTrackImpl.title,
        lastChapterRead = this@getTrackImpl.lastChapterRead.toDouble(),
        totalChapters = this@getTrackImpl.totalChapters.toLong(),
        score = this@getTrackImpl.score.toDouble(),
        status = this@getTrackImpl.status.toLong(),
        startDate = this@getTrackImpl.startedReadingDate,
        finishDate = this@getTrackImpl.finishedReadingDate,
        remoteUrl = this@getTrackImpl.trackingUrl,
        private = this@getTrackImpl.private,
    )
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
