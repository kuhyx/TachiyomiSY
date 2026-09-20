package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.core.common.extensions.JsonObjectEmptyBytes
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.manga.model.Manga

private const val BACKUP_MANGA_SOURCE = 1
private const val BACKUP_MANGA_URL = 2
private const val BACKUP_MANGA_TITLE = 3
private const val BACKUP_MANGA_ARTIST = 4
private const val BACKUP_MANGA_AUTHOR = 5
private const val BACKUP_MANGA_DESCRIPTION = 6
private const val BACKUP_MANGA_GENRE = 7
private const val BACKUP_MANGA_STATUS = 8
private const val BACKUP_MANGA_THUMBNAIL_URL = 9
private const val BACKUP_MANGA_DATE_ADDED = 13
private const val BACKUP_MANGA_VIEWER = 14
private const val BACKUP_MANGA_CHAPTERS = 16
private const val BACKUP_MANGA_CATEGORIES = 17
private const val BACKUP_MANGA_TRACKING = 18
private const val BACKUP_MANGA_FAVORITE = 100
private const val BACKUP_MANGA_CHAPTER_FLAGS = 101
private const val BACKUP_MANGA_VIEWER_FLAGS = 103
private const val BACKUP_MANGA_HISTORY = 104
private const val BACKUP_MANGA_UPDATE_STRATEGY = 105
private const val BACKUP_MANGA_LAST_MODIFIED_AT = 106
private const val BACKUP_MANGA_FAVORITE_MODIFIED_AT = 107
private const val BACKUP_MANGA_EXCLUDED_SCANLATORS = 108
private const val BACKUP_MANGA_VERSION = 109
private const val BACKUP_MANGA_NOTES = 110
private const val BACKUP_MANGA_INITIALIZED = 111
private const val BACKUP_MANGA_MEMO = 112
private const val BACKUP_MANGA_MERGED_MANGA_REFERENCES = 600
private const val BACKUP_MANGA_FLAT_METADATA = 601
private const val BACKUP_MANGA_CUSTOM_STATUS = 602
private const val BACKUP_MANGA_CUSTOM_THUMBNAIL_URL = 603
private const val BACKUP_MANGA_CUSTOM_TITLE = 800
private const val BACKUP_MANGA_CUSTOM_ARTIST = 801
private const val BACKUP_MANGA_CUSTOM_AUTHOR = 802
private const val BACKUP_MANGA_CUSTOM_DESCRIPTION = 804
private const val BACKUP_MANGA_CUSTOM_GENRE = 805

@Suppress("DEPRECATION")
@Serializable
internal class BackupManga(
    // in 1.x some of these values have different names
    @ProtoNumber(BACKUP_MANGA_SOURCE) var source: Long,
    // url is called key in 1.x
    @ProtoNumber(BACKUP_MANGA_URL) var url: String,
    @ProtoNumber(BACKUP_MANGA_TITLE) var title: String = "",
    @ProtoNumber(BACKUP_MANGA_ARTIST) var artist: String? = null,
    @ProtoNumber(BACKUP_MANGA_AUTHOR) var author: String? = null,
    @ProtoNumber(BACKUP_MANGA_DESCRIPTION) var description: String? = null,
    @ProtoNumber(BACKUP_MANGA_GENRE) var genre: List<String> = emptyList(),
    @ProtoNumber(BACKUP_MANGA_STATUS) var status: Int = 0,
    // thumbnailUrl is called cover in 1.x
    @ProtoNumber(BACKUP_MANGA_THUMBNAIL_URL) var thumbnailUrl: String? = null,
    // @ProtoNumber(10) val customCover: String = "", 1.x value, not used in 0.x
    // @ProtoNumber(11) val lastUpdate: Long = 0, 1.x value, not used in 0.x
    // @ProtoNumber(12) val lastInit: Long = 0, 1.x value, not used in 0.x
    @ProtoNumber(BACKUP_MANGA_DATE_ADDED) var dateAdded: Long = 0,
    @ProtoNumber(BACKUP_MANGA_VIEWER) var viewer: Int = 0, // Replaced by viewer_flags
    // @ProtoNumber(15) val flags: Int = 0, 1.x value, not used in 0.x
    @ProtoNumber(BACKUP_MANGA_CHAPTERS) var chapters: List<BackupChapter> = emptyList(),
    @ProtoNumber(BACKUP_MANGA_CATEGORIES) var categories: List<Long> = emptyList(),
    @ProtoNumber(BACKUP_MANGA_TRACKING) var tracking: List<BackupTracking> = emptyList(),
    // Bump by 100 for values that are not saved/implemented in 1.x but are used in 0.x
    @ProtoNumber(BACKUP_MANGA_FAVORITE) var favorite: Boolean = true,
    @ProtoNumber(BACKUP_MANGA_CHAPTER_FLAGS) var chapterFlags: Int = 0,
    // @ProtoNumber(102) var brokenHistory, legacy history model with non-compliant proto number
    @ProtoNumber(BACKUP_MANGA_VIEWER_FLAGS) var viewerFlags: Int? = null,
    @ProtoNumber(BACKUP_MANGA_HISTORY) var history: List<BackupHistory> = emptyList(),
    @ProtoNumber(BACKUP_MANGA_UPDATE_STRATEGY) var updateStrategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
    @ProtoNumber(BACKUP_MANGA_LAST_MODIFIED_AT) var lastModifiedAt: Long = 0,
    @ProtoNumber(BACKUP_MANGA_FAVORITE_MODIFIED_AT) var favoriteModifiedAt: Long? = null,
    // Mihon values start here
    @ProtoNumber(BACKUP_MANGA_EXCLUDED_SCANLATORS) var excludedScanlators: List<String> = emptyList(),
    @ProtoNumber(BACKUP_MANGA_VERSION) var version: Long = 0,
    @ProtoNumber(BACKUP_MANGA_NOTES) var notes: String = "",
    @ProtoNumber(BACKUP_MANGA_INITIALIZED) var initialized: Boolean = false,
    @ProtoNumber(BACKUP_MANGA_MEMO) var memo: ByteArray = JsonObjectEmptyBytes,

    // SY specific values
    @ProtoNumber(BACKUP_MANGA_MERGED_MANGA_REFERENCES) var mergedMangaReferences: List<BackupMergedMangaReference> =
        emptyList(),
    @ProtoNumber(BACKUP_MANGA_FLAT_METADATA) var flatMetadata: BackupFlatMetadata? = null,
    @ProtoNumber(BACKUP_MANGA_CUSTOM_STATUS) var customStatus: Int = 0,
    @ProtoNumber(BACKUP_MANGA_CUSTOM_THUMBNAIL_URL) var customThumbnailUrl: String? = null,

    // J2K specific values
    @ProtoNumber(BACKUP_MANGA_CUSTOM_TITLE) var customTitle: String? = null,
    @ProtoNumber(BACKUP_MANGA_CUSTOM_ARTIST) var customArtist: String? = null,
    @ProtoNumber(BACKUP_MANGA_CUSTOM_AUTHOR) var customAuthor: String? = null,
    // skipping 803 due to using duplicate value in previous builds
    @ProtoNumber(BACKUP_MANGA_CUSTOM_DESCRIPTION) var customDescription: String? = null,
    @ProtoNumber(BACKUP_MANGA_CUSTOM_GENRE) var customGenre: List<String>? = null,

) {
    fun getMangaImpl(): Manga {
        return Manga.create().copy(
            url = this@BackupManga.url,
            // SY -->
            ogTitle = this@BackupManga.title,
            ogArtist = this@BackupManga.artist,
            ogAuthor = this@BackupManga.author,
            ogThumbnailUrl = this@BackupManga.thumbnailUrl,
            ogDescription = this@BackupManga.description,
            ogGenre = this@BackupManga.genre,
            ogStatus = this@BackupManga.status.toLong(),
            // SY <--
            favorite = this@BackupManga.favorite,
            source = this@BackupManga.source,
            dateAdded = this@BackupManga.dateAdded,
            viewerFlags = (this@BackupManga.viewerFlags ?: this@BackupManga.viewer).toLong(),
            chapterFlags = this@BackupManga.chapterFlags.toLong(),
            updateStrategy = this@BackupManga.updateStrategy,
            lastModifiedAt = this@BackupManga.lastModifiedAt,
            favoriteModifiedAt = this@BackupManga.favoriteModifiedAt,
            version = this@BackupManga.version,
            notes = this@BackupManga.notes,
            initialized = this@BackupManga.initialized,
            memo = MemoColumnAdapter.decode(this@BackupManga.memo),
        )
    }
}
