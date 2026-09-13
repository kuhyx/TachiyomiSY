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

@Serializable
public class NHentaiSearchMetadata : RaisedSearchMetadata() {
    public var url: String? get() = nhId?.let { BASE_URL + nhIdToPath(it) }
        set(a) {
            a?.let {
                nhId = nhUrlToId(a)
            }
        }

    public var nhId: Long? = null

    public var uploadDate: Long? = null

    public var favoritesCount: Long? = null

    public var mediaId: String? = null

    public var japaneseTitle: String? by titleDelegate(TITLE_TYPE_JAPANESE)
    public var englishTitle: String? by titleDelegate(TITLE_TYPE_ENGLISH)
    public var shortTitle: String? by titleDelegate(TITLE_TYPE_SHORT)

    public var coverImageUrl: String? = null
    public var pageImagePreviewUrls: List<String> = emptyList()

    public var scanlator: String? = null

    public var preferredTitle: Int? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = nhId?.let { nhIdToPath(it) }

        val title = when (preferredTitle) {
            TITLE_TYPE_SHORT -> shortTitle ?: englishTitle ?: japaneseTitle ?: manga.title
            0, TITLE_TYPE_ENGLISH -> englishTitle ?: japaneseTitle ?: shortTitle ?: manga.title
            else -> englishTitle ?: japaneseTitle ?: shortTitle ?: manga.title
        }

        // Set artist (if we can find one)
        val artist = tags.ofNamespace(NHENTAI_ARTIST_NAMESPACE).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Set group (if we can find one)
        val group = tags.ofNamespace(NHENTAI_GROUP_NAMESPACE).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Copy tags -> genres
        val genres = tagsToGenreString()

        // Try to automatically identify if it is ongoing, we try not to be too lenient here to avoid making mistakes
        // We default to completed
        var status = SManga.COMPLETED
        englishTitle?.let { t ->
            MetadataUtil.ONGOING_SUFFIX.find {
                t.endsWith(it, ignoreCase = true)
            }?.let {
                status = SManga.ONGOING
            }
        }

        return manga.copy(
            url = key ?: manga.url,
            thumbnail_url = coverImageUrl ?: manga.thumbnail_url,
            title = title,
            artist = group ?: manga.artist,
            author = artist ?: manga.artist,
            genre = genres,
            status = status,
            description = null,
        )
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

    public companion object {
        private const val TITLE_TYPE_JAPANESE = 0
        public const val TITLE_TYPE_ENGLISH: Int = 1
        public const val TITLE_TYPE_SHORT: Int = 2

        public const val TAG_TYPE_DEFAULT: Int = 0

        public const val BASE_URL: String = "https://nhentai.net"

        private const val NHENTAI_ARTIST_NAMESPACE = "artist"
        private const val NHENTAI_GROUP_NAMESPACE = "group"
        public const val NHENTAI_CATEGORIES_NAMESPACE: String = "category"

        public fun nhUrlToId(url: String): Long =
            url.split("/").last { it.isNotBlank() }.toLong()

        public fun nhIdToPath(id: Long): String = "/g/$id/"
    }
}
