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

@Serializable
public class EHentaiSearchMetadata : RaisedSearchMetadata() {
    public var gId: String?
        get() = indexedExtra
        set(value) {
            indexedExtra = value
        }

    public var gToken: String? = null
    public var exh: Boolean? = null
    public var thumbnailUrl: String? = null

    public var title: String? by titleDelegate(TITLE_TYPE_TITLE)
    public var altTitle: String? by titleDelegate(TITLE_TYPE_ALT_TITLE)

    public var genre: String? = null

    public var datePosted: Long? = null
    public var parent: String? = null

    public var visible: String? = null // Not a boolean
    public var language: String? = null
    public var translated: Boolean? = null
    public var size: Long? = null
    public var length: Int? = null
    public var favorites: Int? = null
    public var ratingCount: Int? = null
    public var averageRating: Double? = null

    public var aged: Boolean = false
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
            thumbnail_url = cover ?: manga.thumbnail_url,
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

    public companion object {
        private const val TITLE_TYPE_TITLE = 0
        private const val TITLE_TYPE_ALT_TITLE = 1

        public const val TAG_TYPE_NORMAL: Int = 0
        public const val TAG_TYPE_LIGHT: Int = 1
        public const val TAG_TYPE_WEAK: Int = 2

        public const val EH_GENRE_NAMESPACE: String = "genre"
        private const val EH_ARTIST_NAMESPACE = "artist"
        private const val EH_GROUP_NAMESPACE = "group"
        public const val EH_LANGUAGE_NAMESPACE: String = "language"
        public const val EH_META_NAMESPACE: String = "meta"
        public const val EH_UPLOADER_NAMESPACE: String = "uploader"
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

        public fun galleryId(url: String): String = splitGalleryUrl(url)[1]

        public fun galleryToken(url: String): String =
            splitGalleryUrl(url)[2]

        public fun normalizeUrl(url: String): String =
            idAndTokenToUrl(galleryId(url), galleryToken(url))

        public fun idAndTokenToUrl(id: String, token: String): String =
            "/g/$id/$token/?nw=always"
    }
}
