package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Gallery metadata scraped from nhentai. */
@Serializable
public class NHentaiSearchMetadata : RaisedSearchMetadata() {
    /** Relative gallery URL derived from [nhId]; setting it parses the id back. */
    public var url: String? get() = nhId?.let { BASE_URL + nhIdToPath(it) }
        set(a) {
            a?.let {
                nhId = nhUrlToId(a)
            }
        }

    /** Gallery id. */
    public var nhId: Long? = null

    /** Upload time in epoch seconds. */
    public var uploadDate: Long? = null

    /** Favourite count. */
    public var favoritesCount: Long? = null

    /** Media id used for image URLs. */
    public var mediaId: String? = null

    /** Japanese title. */
    public var japaneseTitle: String? by titleDelegate(TITLE_TYPE_JAPANESE)

    /** English title. */
    public var englishTitle: String? by titleDelegate(TITLE_TYPE_ENGLISH)

    /** Pretty (short) title. */
    public var shortTitle: String? by titleDelegate(TITLE_TYPE_SHORT)

    /** Cover URL. */
    public var coverImageUrl: String? = null

    /** Thumbnail URL per page. */
    public var pageImagePreviewUrls: List<String> = emptyList()

    /** Scanlator credit. */
    public var scanlator: String? = null

    /** Which title type to show, one of the `TITLE_TYPE_*` constants. */
    public var preferredTitle: Int? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = nhId?.let { nhIdToPath(it) }

        val title = preferredTitle(manga.title)
        val artist = joinedTags(NHENTAI_ARTIST_NAMESPACE)
        val group = joinedTags(NHENTAI_GROUP_NAMESPACE)

        return manga.copy(
            url = key ?: manga.url,
            thumbnail_url = coverImageUrl ?: manga.thumbnail_url,
            title = title,
            artist = group ?: manga.artist,
            author = artist ?: manga.artist,
            genre = tagsToGenreString(),
            status = guessedStatus(),
            description = null,
        )
    }

    private fun preferredTitle(fallback: String): String = if (preferredTitle == TITLE_TYPE_SHORT) {
        shortTitle ?: englishTitle ?: japaneseTitle ?: fallback
    } else {
        englishTitle ?: japaneseTitle ?: shortTitle ?: fallback
    }

    private fun joinedTags(namespace: String): String? =
        tags.ofNamespace(namespace).ifEmpty { null }?.joinToString { it.name }

    // Only an explicit "ongoing" marker in the title counts; completed is the safe default.
    private fun guessedStatus(): Int {
        val title = englishTitle ?: return SManga.COMPLETED
        val ongoing = MetadataUtil.ONGOING_SUFFIX.any { title.endsWith(it, ignoreCase = true) }
        return if (ongoing) SManga.ONGOING else SManga.COMPLETED
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(nhId) { stringResource(SYMR.strings.id) },
                getItem(
                    uploadDate,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()))
                    },
                ) {
                    stringResource(SYMR.strings.date_posted)
                },
                getItem(favoritesCount) { stringResource(SYMR.strings.total_favorites) },
                getItem(mediaId) { stringResource(SYMR.strings.media_id) },
                getItem(japaneseTitle) { stringResource(SYMR.strings.japanese_title) },
                getItem(englishTitle) { stringResource(SYMR.strings.english_title) },
                getItem(shortTitle) { stringResource(SYMR.strings.short_title) },
                getItem(coverImageUrl) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(pageImagePreviewUrls.size) { stringResource(SYMR.strings.page_count) },
                getItem(scanlator) { stringResource(MR.strings.scanlator) },
            )
        }
    }

    /** Constants and URL helpers. */
    public companion object {
        private const val TITLE_TYPE_JAPANESE = 0

        /** Title type of [englishTitle]. */
        public const val TITLE_TYPE_ENGLISH: Int = 1

        /** Title type of [shortTitle]. */
        public const val TITLE_TYPE_SHORT: Int = 2

        /** Type of every scraped tag. */
        public const val TAG_TYPE_DEFAULT: Int = 0

        /** Site root. */
        public const val BASE_URL: String = "https://nhentai.net"

        private const val NHENTAI_ARTIST_NAMESPACE = "artist"
        private const val NHENTAI_GROUP_NAMESPACE = "group"

        /** Namespace of category tags. */
        public const val NHENTAI_CATEGORIES_NAMESPACE: String = "category"

        /** The gallery id at the end of a gallery [url]. */
        public fun nhUrlToId(url: String): Long =
            url.split("/").last { it.isNotBlank() }.toLong()

        /** The relative gallery path for [id]. */
        public fun nhIdToPath(id: Long): String = "/g/$id/"
    }
}
