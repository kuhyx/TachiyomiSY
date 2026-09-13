package exh.metadata.metadata

import android.content.Context
import androidx.core.net.toUri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import exh.pref.DelegateSourcePreferences
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Gallery metadata scraped from E-Hentai / ExHentai. */
@Serializable
public class EHentaiSearchMetadata : RaisedSearchMetadata() {
    /** Gallery id; stored as the indexed extra so it is searchable. */
    public var gId: String?
        get() = indexedExtra
        set(value) {
            indexedExtra = value
        }

    /** Gallery token, the second part of the gallery URL. */
    public var gToken: String? = null

    /** True when the gallery came from ExHentai. */
    public var exh: Boolean? = null

    /** Cover URL. */
    public var thumbnailUrl: String? = null

    /** Main (romanised) title. */
    public var title: String? by titleDelegate(TITLE_TYPE_TITLE)

    /** Alternative (usually Japanese) title. */
    public var altTitle: String? by titleDelegate(TITLE_TYPE_ALT_TITLE)

    /** Site category, such as Doujinshi. */
    public var genre: String? = null

    /** Upload time in epoch milliseconds. */
    public var datePosted: Long? = null

    /** URL of the gallery this one replaces, if any. */
    public var parent: String? = null

    /** Site visibility text; not a boolean. */
    public var visible: String? = null // Not a boolean

    /** Language tag. */
    public var language: String? = null

    /** True when tagged as a translation. */
    public var translated: Boolean? = null

    /** Archive size in bytes. */
    public var size: Long? = null

    /** Page count. */
    public var length: Int? = null

    /** Favourite count. */
    public var favorites: Int? = null

    /** Number of ratings. */
    public var ratingCount: Int? = null

    /** Average rating. */
    public var averageRating: Double? = null

    /** True when the gallery is old enough to be pruned from update checks. */
    public var aged: Boolean = false

    /** Epoch milliseconds of the last update check. */
    public var lastUpdateCheck: Long = 0

    override fun createMangaInfo(manga: SManga): SManga {
        val key = gId?.let { gId ->
            gToken?.let { gToken ->
                idAndTokenToUrl(gId, gToken)
            }
        }
        val cover = thumbnailUrl

        // No title bug?
        val title = altTitle
            ?.takeIf { Injekt.get<DelegateSourcePreferences>().useJapaneseTitle.get() } // todo
            ?: title

        // Set artist (if we can find one)
        val artist = tags.ofNamespace(EH_ARTIST_NAMESPACE)
            .ifEmpty { null }
            ?.joinToString { it.name }

        // Set group (if we can find one)
        val group = tags.ofNamespace(EH_GROUP_NAMESPACE)
            .ifEmpty { null }
            ?.joinToString { it.name }

        // Copy tags -> genres
        val genres = tagsToGenreString()

        // Try to automatically identify if it is ongoing, we try not to be too lenient here to avoid making mistakes
        // We default to completed
        var status = SManga.COMPLETED
        title?.let { t ->
            MetadataUtil.ONGOING_SUFFIX.find {
                t.endsWith(it, ignoreCase = true)
            }?.let {
                status = SManga.ONGOING
            }
        }

        return manga.copy(
            url = key ?: manga.url,
            title = title ?: manga.title,
            artist = group ?: manga.artist,
            author = artist ?: manga.artist,
            description = null,
            genre = genres,
            status = status,
            thumbnailUrl = cover ?: manga.thumbnail_url,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(gId) { stringResource(SYMR.strings.id) },
                getItem(gToken) { stringResource(SYMR.strings.token) },
                getItem(exh) { stringResource(SYMR.strings.is_exhentai_gallery) },
                getItem(thumbnailUrl) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(altTitle) { stringResource(SYMR.strings.alt_title) },
                getItem(genre) { stringResource(SYMR.strings.genre) },
                getItem(
                    datePosted,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                    },
                ) {
                    stringResource(SYMR.strings.date_posted)
                },
                getItem(parent) { stringResource(SYMR.strings.parent) },
                getItem(visible) { stringResource(SYMR.strings.visible) },
                getItem(language) { stringResource(SYMR.strings.language) },
                getItem(translated) { stringResource(SYMR.strings.translated) },
                getItem(size, { MetadataUtil.humanReadableByteCount(it, true) }) {
                    stringResource(SYMR.strings.gallery_size)
                },
                getItem(length) { stringResource(SYMR.strings.page_count) },
                getItem(favorites) { stringResource(SYMR.strings.total_favorites) },
                getItem(ratingCount) { stringResource(SYMR.strings.total_ratings) },
                getItem(averageRating) { stringResource(SYMR.strings.average_rating) },
                getItem(aged) { stringResource(SYMR.strings.aged) },
                getItem(
                    lastUpdateCheck,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                    },
                ) { stringResource(SYMR.strings.last_update_check) },
            )
        }
    }

    /** Tag types, namespaces and URL helpers. */
    public companion object {
        private const val TITLE_TYPE_TITLE = 0
        private const val TITLE_TYPE_ALT_TITLE = 1

        /** A regular tag. */
        public const val TAG_TYPE_NORMAL: Int = 0

        /** A tag the site marks as weak but likely. */
        public const val TAG_TYPE_LIGHT: Int = 1

        /** A tag the site marks as weak. */
        public const val TAG_TYPE_WEAK: Int = 2

        /** Namespace of the category tag. */
        public const val EH_GENRE_NAMESPACE: String = "genre"
        private const val EH_ARTIST_NAMESPACE = "artist"
        private const val EH_GROUP_NAMESPACE = "group"

        /** Namespace of language tags. */
        public const val EH_LANGUAGE_NAMESPACE: String = "language"

        /** Namespace of site meta tags. */
        public const val EH_META_NAMESPACE: String = "meta"

        /** Namespace of the uploader tag. */
        public const val EH_UPLOADER_NAMESPACE: String = "uploader"

        /** Namespace of the visibility tag. */
        public const val EH_VISIBILITY_NAMESPACE: String = "visibility"

        private fun splitGalleryUrl(url: String) =
            url.let {
                // Only parse URL if is full URL
                val pathSegments = if (it.startsWith("http")) {
                    it.toUri().pathSegments
                } else {
                    it.split('/')
                }
                pathSegments.filterNot(String::isNullOrBlank)
            }

        /** The gallery id segment of a gallery [url]. */
        public fun galleryId(url: String): String = splitGalleryUrl(url)[1]

        /** The gallery token segment of a gallery [url]. */
        public fun galleryToken(url: String): String =
            splitGalleryUrl(url)[2]

        /** The canonical relative form of a gallery [url]. */
        public fun normalizeUrl(url: String): String =
            idAndTokenToUrl(galleryId(url), galleryToken(url))

        /** The relative gallery URL for an [id] and [token], skipping the content warning. */
        public fun idAndTokenToUrl(id: String, token: String): String =
            "/g/$id/$token/?nw=always"
    }
}
