package exh.metadata.metadata

import android.content.Context
import androidx.core.net.toUri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import exh.util.nullIfEmpty
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Serializable
public class TsuminoSearchMetadata : RaisedSearchMetadata() {
    public var tmId: Int? = null

    public var title: String? by titleDelegate(TITLE_TYPE_MAIN)

    public var artist: String? = null

    public var uploadDate: Long? = null

    public var length: Int? = null

    public var ratingString: String? = null

    public var averageRating: Float? = null

    public var userRatings: Long? = null

    public var favorites: Long? = null

    public var category: String? = null

    public var collection: String? = null

    public var group: String? = null

    public var parody: List<String> = emptyList()

    public var character: List<String> = emptyList()

    override fun createMangaInfo(manga: SManga): SManga {
        val title = title
        val cover = tmId?.let { BASE_URL.replace("www", "content") + thumbUrlFromId(it.toString()) }

        val artist = artist

        val status = SManga.UNKNOWN

        // Copy tags -> genres
        val genres = tagsToGenreString()

        val description = "meta"

        return manga.copy(
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            artist = artist ?: manga.artist,
            status = status,
            genre = genres,
            description = description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(tmId) { stringResource(SYMR.strings.id) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(uploader) { stringResource(SYMR.strings.uploader) },
                getItem(
                    uploadDate,
                    {
                        MetadataUtil.EX_DATE_FORMAT
                            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                    },
                ) {
                    stringResource(SYMR.strings.date_posted)
                },
                getItem(length) { stringResource(SYMR.strings.page_count) },
                getItem(ratingString) { stringResource(SYMR.strings.rating_string) },
                getItem(averageRating) { stringResource(SYMR.strings.average_rating) },
                getItem(userRatings) { stringResource(SYMR.strings.total_ratings) },
                getItem(favorites) { stringResource(SYMR.strings.total_favorites) },
                getItem(category) { stringResource(SYMR.strings.genre) },
                getItem(collection) { stringResource(SYMR.strings.collection) },
                getItem(group) { stringResource(SYMR.strings.group) },
                getItem(parody.nullIfEmpty(), { it.joinToString() }) { stringResource(SYMR.strings.parodies) },
                getItem(character.nullIfEmpty(), { it.joinToString() }) { stringResource(SYMR.strings.characters) },
            )
        }
    }

    public companion object {
        private const val TITLE_TYPE_MAIN = 0

        public const val TAG_TYPE_DEFAULT: Int = 0

        public val BASE_URL: String = "https://www.tsumino.com"

        public val TSUMINO_DATE_FORMAT: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        public fun tmIdFromUrl(url: String): String? = url.toUri().lastPathSegment

        public fun thumbUrlFromId(id: String): String = "/thumbs/$id/1"
    }
}
