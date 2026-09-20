package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.core.common.extensions.JsonObjectEmptyBytes
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.chapter.model.Chapter

private const val BACKUP_CHAPTER_URL = 1
private const val BACKUP_CHAPTER_NAME = 2
private const val BACKUP_CHAPTER_SCANLATOR = 3
private const val BACKUP_CHAPTER_READ = 4
private const val BACKUP_CHAPTER_BOOKMARK = 5
private const val BACKUP_CHAPTER_LAST_PAGE_READ = 6
private const val BACKUP_CHAPTER_DATE_FETCH = 7
private const val BACKUP_CHAPTER_DATE_UPLOAD = 8
private const val BACKUP_CHAPTER_CHAPTER_NUMBER = 9
private const val BACKUP_CHAPTER_SOURCE_ORDER = 10
private const val BACKUP_CHAPTER_LAST_MODIFIED_AT = 11
private const val BACKUP_CHAPTER_VERSION = 12
private const val BACKUP_CHAPTER_MEMO = 13

@Serializable
internal class BackupChapter(
    // in 1.x some of these values have different names
    // url is called key in 1.x
    @ProtoNumber(BACKUP_CHAPTER_URL) var url: String,
    @ProtoNumber(BACKUP_CHAPTER_NAME) var name: String,
    @ProtoNumber(BACKUP_CHAPTER_SCANLATOR) var scanlator: String? = null,
    @ProtoNumber(BACKUP_CHAPTER_READ) var read: Boolean = false,
    @ProtoNumber(BACKUP_CHAPTER_BOOKMARK) var bookmark: Boolean = false,
    // lastPageRead is called progress in 1.x
    @ProtoNumber(BACKUP_CHAPTER_LAST_PAGE_READ) var lastPageRead: Long = 0,
    @ProtoNumber(BACKUP_CHAPTER_DATE_FETCH) var dateFetch: Long = 0,
    @ProtoNumber(BACKUP_CHAPTER_DATE_UPLOAD) var dateUpload: Long = 0,
    // chapterNumber is called number is 1.x
    @ProtoNumber(BACKUP_CHAPTER_CHAPTER_NUMBER) var chapterNumber: Float = 0F,
    @ProtoNumber(BACKUP_CHAPTER_SOURCE_ORDER) var sourceOrder: Long = 0,
    @ProtoNumber(BACKUP_CHAPTER_LAST_MODIFIED_AT) var lastModifiedAt: Long = 0,
    @ProtoNumber(BACKUP_CHAPTER_VERSION) var version: Long = 0,
    @ProtoNumber(BACKUP_CHAPTER_MEMO) var memo: ByteArray = JsonObjectEmptyBytes,
) {
    fun toChapterImpl(): Chapter {
        return Chapter.create().copy(
            url = this@BackupChapter.url,
            name = this@BackupChapter.name,
            chapterNumber = this@BackupChapter.chapterNumber.toDouble(),
            scanlator = this@BackupChapter.scanlator,
            read = this@BackupChapter.read,
            bookmark = this@BackupChapter.bookmark,
            lastPageRead = this@BackupChapter.lastPageRead,
            dateFetch = this@BackupChapter.dateFetch,
            dateUpload = this@BackupChapter.dateUpload,
            sourceOrder = this@BackupChapter.sourceOrder,
            lastModifiedAt = this@BackupChapter.lastModifiedAt,
            version = this@BackupChapter.version,
            memo = MemoColumnAdapter.decode(this@BackupChapter.memo),
        )
    }
}

internal val backupChapterMapper = {
        _: Long,
        _: Long,
        url: String,
        name: String,
        scanlator: String?,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        chapterNumber: Double,
        sourceOrder: Long,
        dateFetch: Long,
        dateUpload: Long,
        lastModifiedAt: Long,
        version: Long,
        _: Long,
        memo: JsonObject,
    ->
    BackupChapter(
        url = url,
        name = name,
        chapterNumber = chapterNumber.toFloat(),
        scanlator = scanlator,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        sourceOrder = sourceOrder,
        lastModifiedAt = lastModifiedAt,
        version = version,
        memo = MemoColumnAdapter.encode(memo),
    )
}
