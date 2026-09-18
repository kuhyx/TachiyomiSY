package tachiyomi.domain.manga.model

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.serialization.ProxiedJavaSerializable
import uy.kohesive.injekt.injectLazy
import java.io.ObjectStreamException
import java.io.Serializable as JavaSerializable

/**
 * A manga row. The `og*` fields are what the source reported; [title] and
 * siblings apply the user's edits ([CustomMangaInfo]) on top of them for
 * favourites. Chapter-flag views live in `MangaChapterFlags.kt`.
 *
 * @property id Row id; -1 until inserted.
 * @property source Id of the source this manga comes from.
 * @property favorite Whether it is in the library.
 * @property lastUpdate Epoch millis of the last chapter fetch.
 * @property nextUpdate Epoch millis when the next chapter is expected.
 * @property fetchInterval Days between expected chapters; negative when set by the user.
 * @property dateAdded Epoch millis the manga was added to the library.
 * @property viewerFlags Reader mode and orientation bits.
 * @property chapterFlags Chapter sort, display and filter bits ([CHAPTER_SORTING_MASK] and siblings).
 * @property coverLastModified Epoch millis of the last cover change, for cache keys.
 * @property url Path of the manga on its source.
 * @property ogTitle Title as reported by the source.
 * @property ogArtist Artist as reported by the source.
 * @property ogAuthor Author as reported by the source.
 * @property ogThumbnailUrl Cover url as reported by the source.
 * @property ogDescription Description as reported by the source.
 * @property ogGenre Genres as reported by the source.
 * @property ogStatus Publishing status as reported by the source.
 * @property updateStrategy Whether library updates fetch new chapters.
 * @property initialized Whether details have been fetched at least once.
 * @property lastModifiedAt Epoch seconds of the last row change, for sync.
 * @property favoriteModifiedAt Epoch seconds the favourite flag last changed, for sync.
 * @property version Sync version counter.
 * @property notes The user's free-text notes.
 * @property memo Structured per-manga data extensions may keep.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Immutable
public data class Manga(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val dateAdded: Long,
    val viewerFlags: Long,
    val chapterFlags: Long,
    val coverLastModified: Long,
    val url: String,
    // SY -->
    val ogTitle: String,
    val ogArtist: String?,
    val ogAuthor: String?,
    val ogThumbnailUrl: String?,
    val ogDescription: String?,
    val ogGenre: List<String>?,
    val ogStatus: Long,
    // SY <--
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,
    val favoriteModifiedAt: Long?,
    val version: Long,
    val notes: String,
    val memo: JsonObject,
) : ProxiedJavaSerializable() {

    // SY -->
    @Contextual
    private val customMangaInfo = if (favorite) {
        getCustomMangaInfo.get(id)
    } else {
        null
    }

    /** [ogTitle] unless the user edited it. */
    val title: String
        get() = customMangaInfo?.title ?: ogTitle

    /** [ogAuthor] unless the user edited it. */
    val author: String?
        get() = customMangaInfo?.author ?: ogAuthor

    /** [ogArtist] unless the user edited it. */
    val artist: String?
        get() = customMangaInfo?.artist ?: ogArtist

    /** [ogThumbnailUrl] unless the user edited it. */
    val thumbnailUrl: String?
        get() = customMangaInfo?.thumbnailUrl ?: ogThumbnailUrl

    /** [ogDescription] unless the user edited it. */
    val description: String?
        get() = customMangaInfo?.description ?: ogDescription

    /** [ogGenre] unless the user edited it. */
    val genre: List<String>?
        get() = customMangaInfo?.genre ?: ogGenre

    /** [ogStatus] unless the user edited it. */
    val status: Long
        get() = customMangaInfo?.status ?: ogStatus
    // SY <--

    override fun writeReplacement(): JavaSerializable = JavaToKotlinXSerializable(Json.encodeToString<Manga>(this))

    /** The Java-serialized form of a [Manga]: its JSON, decoded again on read. */
    public open class JavaToKotlinXSerializable(private val data: String) : JavaSerializable {

        /** Java serialization hook: decodes the JSON back into the [Manga]. */
        @Throws(ObjectStreamException::class)
        protected fun readResolve(): Any = Json.decodeFromString<Manga>(data)
    }

    /** The chapter-flag bit layout and the factory for a blank manga. */
    public companion object {
        /** A filter value that keeps every chapter. */
        public const val SHOW_ALL: Long = 0x00000000L

        /** Sort direction bit: newest first. */
        public const val CHAPTER_SORT_DESC: Long = 0x00000000L

        /** Sort direction bit: oldest first. */
        public const val CHAPTER_SORT_ASC: Long = 0x00000001L

        /** Mask of the sort direction bit. */
        public const val CHAPTER_SORT_DIR_MASK: Long = 0x00000001L

        /** Read filter: unread chapters only. */
        public const val CHAPTER_SHOW_UNREAD: Long = 0x00000002L

        /** Read filter: read chapters only. */
        public const val CHAPTER_SHOW_READ: Long = 0x00000004L

        /** Mask of the read filter bits. */
        public const val CHAPTER_UNREAD_MASK: Long = 0x00000006L

        /** Download filter: downloaded chapters only. */
        public const val CHAPTER_SHOW_DOWNLOADED: Long = 0x00000008L

        /** Download filter: chapters not downloaded only. */
        public const val CHAPTER_SHOW_NOT_DOWNLOADED: Long = 0x00000010L

        /** Mask of the download filter bits. */
        public const val CHAPTER_DOWNLOADED_MASK: Long = 0x00000018L

        /** Bookmark filter: bookmarked chapters only. */
        public const val CHAPTER_SHOW_BOOKMARKED: Long = 0x00000020L

        /** Bookmark filter: chapters not bookmarked only. */
        public const val CHAPTER_SHOW_NOT_BOOKMARKED: Long = 0x00000040L

        /** Mask of the bookmark filter bits. */
        public const val CHAPTER_BOOKMARKED_MASK: Long = 0x00000060L

        /** Sort key: the source's order. */
        public const val CHAPTER_SORTING_SOURCE: Long = 0x00000000L

        /** Sort key: chapter number. */
        public const val CHAPTER_SORTING_NUMBER: Long = 0x00000100L

        /** Sort key: upload date. */
        public const val CHAPTER_SORTING_UPLOAD_DATE: Long = 0x00000200L

        /** Sort key: chapter name. */
        public const val CHAPTER_SORTING_ALPHABET: Long = 0x00000300L

        /** Mask of the sort key bits. */
        public const val CHAPTER_SORTING_MASK: Long = 0x00000300L

        /** Display mode: chapter name. */
        public const val CHAPTER_DISPLAY_NAME: Long = 0x00000000L

        /** Display mode: chapter number. */
        public const val CHAPTER_DISPLAY_NUMBER: Long = 0x00100000L

        /** Mask of the display mode bit. */
        public const val CHAPTER_DISPLAY_MASK: Long = 0x00100000L

        // SY -->
        private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()
        // SY <--

        /** An empty, not yet inserted manga. */
        public fun create(): Manga = Manga(
            id = -1L,
            url = "",
            // Sy -->
            ogTitle = "",
            // SY <--
            source = -1L,
            favorite = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
            dateAdded = 0L,
            viewerFlags = 0L,
            chapterFlags = 0L,
            coverLastModified = 0L,
            // SY -->
            ogArtist = null,
            ogAuthor = null,
            ogThumbnailUrl = null,
            ogDescription = null,
            ogGenre = null,
            ogStatus = 0L,
            // SY <--
            updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
            initialized = false,
            lastModifiedAt = 0L,
            favoriteModifiedAt = null,
            version = 0L,
            notes = "",
            memo = JsonObject.EMPTY,
        )
    }
}
