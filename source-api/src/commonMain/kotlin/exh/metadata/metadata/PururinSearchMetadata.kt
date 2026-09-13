package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

@Serializable
public class PururinSearchMetadata : RaisedSearchMetadata() {
    public var prId: Int? = null

    public var prShortLink: String? = null

    public var title: String? by titleDelegate(TITLE_TYPE_TITLE)
    public var altTitle: String? by titleDelegate(TITLE_TYPE_ALT_TITLE)

    public var thumbnailUrl: String? = null

    public var uploaderDisp: String? = null

    public var pages: Int? = null

    public var fileSize: String? = null

    public var ratingCount: Int? = null
    public var averageRating: Double? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = prId?.let { prId ->
            prShortLink?.let { prShortLink ->
                "/gallery/$prId/$prShortLink"
            }
        }

        val title = title ?: altTitle

        val cover = thumbnailUrl

        val artist = tags.ofNamespace(TAG_NAMESPACE_ARTIST).joinToString { it.name }

        val genres = tagsToGenreString()

        val description = "meta"

        return manga.copy(
            url = key ?: manga.url,
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            artist = artist,
            genre = genres,
            description = description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(prId) { stringResource(SYMR.strings.id) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(altTitle) { stringResource(SYMR.strings.alt_title) },
                getItem(thumbnailUrl) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(uploaderDisp) { stringResource(SYMR.strings.uploader_capital) },
                getItem(uploader) { stringResource(SYMR.strings.uploader) },
                getItem(pages) { stringResource(SYMR.strings.page_count) },
                getItem(fileSize) { stringResource(SYMR.strings.gallery_size) },
                getItem(ratingCount) { stringResource(SYMR.strings.total_ratings) },
                getItem(averageRating) { stringResource(SYMR.strings.average_rating) },
            )
        }
    }

    public companion object {
        private const val TITLE_TYPE_TITLE = 0
        private const val TITLE_TYPE_ALT_TITLE = 1

        public const val TAG_TYPE_DEFAULT: Int = 0

        private const val TAG_NAMESPACE_ARTIST = "artist"
        public const val TAG_NAMESPACE_CATEGORY: String = "category"

        public const val BASE_URL: String = "https://pururin.me"
    }
}
