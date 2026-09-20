package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.manga.model.MergedMangaReference

/*
* SY merged manga backup class
 */
private const val BACKUP_MERGED_MANGA_REFERENCE_IS_INFO_MANGA = 1
private const val BACKUP_MERGED_MANGA_REFERENCE_GET_CHAPTER_UPDATES = 2
private const val BACKUP_MERGED_MANGA_REFERENCE_CHAPTER_SORT_MODE = 3
private const val BACKUP_MERGED_MANGA_REFERENCE_CHAPTER_PRIORITY = 4
private const val BACKUP_MERGED_MANGA_REFERENCE_DOWNLOAD_CHAPTERS = 5
private const val BACKUP_MERGED_MANGA_REFERENCE_MERGE_URL = 6
private const val BACKUP_MERGED_MANGA_REFERENCE_MANGA_URL = 7
private const val BACKUP_MERGED_MANGA_REFERENCE_MANGA_SOURCE_ID = 8

@Serializable
internal data class BackupMergedMangaReference(
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_IS_INFO_MANGA) var isInfoManga: Boolean,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_GET_CHAPTER_UPDATES) var getChapterUpdates: Boolean,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_CHAPTER_SORT_MODE) var chapterSortMode: Int,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_CHAPTER_PRIORITY) var chapterPriority: Int,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_DOWNLOAD_CHAPTERS) var downloadChapters: Boolean,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_MERGE_URL) var mergeUrl: String,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_MANGA_URL) var mangaUrl: String,
    @ProtoNumber(BACKUP_MERGED_MANGA_REFERENCE_MANGA_SOURCE_ID) var mangaSourceId: Long,
)

internal fun BackupMergedMangaReference.getMergedMangaReference(): MergedMangaReference {
    return MergedMangaReference(
        isInfoManga = isInfoManga,
        getChapterUpdates = getChapterUpdates,
        chapterSortMode = chapterSortMode,
        chapterPriority = chapterPriority,
        downloadChapters = downloadChapters,
        mergeUrl = mergeUrl,
        mangaUrl = mangaUrl,
        mangaSourceId = mangaSourceId,
        mergeId = null,
        mangaId = null,
        id = -1,
    )
}

internal val backupMergedMangaReferenceMapper =
    {
            _: Long,
            isInfoManga: Boolean,
            getChapterUpdates: Boolean,
            chapterSortMode: Long,
            chapterPriority: Long,
            downloadChapters: Boolean,
            _: Long,
            mergeUrl: String,
            _: Long?,
            mangaUrl: String,
            mangaSourceId: Long,
        ->
        BackupMergedMangaReference(
            isInfoManga = isInfoManga,
            getChapterUpdates = getChapterUpdates,
            chapterSortMode = chapterSortMode.toInt(),
            chapterPriority = chapterPriority.toInt(),
            downloadChapters = downloadChapters,
            mergeUrl = mergeUrl,
            mangaUrl = mangaUrl,
            mangaSourceId = mangaSourceId,
        )
    }
