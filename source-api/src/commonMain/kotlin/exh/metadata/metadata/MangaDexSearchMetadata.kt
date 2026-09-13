package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.md.utils.MangaDexRelation
import exh.metadata.metadata.base.TrackerIdMetadata
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

@Serializable
public class MangaDexSearchMetadata : RaisedSearchMetadata(), TrackerIdMetadata {
    public var mdUuid: String? = null

    // var mdUrl: String? = null

    public var cover: String? = null

    public var title: String? by titleDelegate(TITLE_TYPE_MAIN)
    public var altTitles: List<String>? = null

    public var description: String? = null

    public var authors: List<String>? = null
    public var artists: List<String>? = null

    public var langFlag: String? = null

    public var lastChapterNumber: Int? = null
    public var rating: Float? = null
    // var users: String? = null

    override var anilistId: String? = null
    override var kitsuId: String? = null
    override var myAnimeListId: String? = null
    override var mangaUpdatesId: String? = null
    override var animePlanetId: String? = null

    public var status: Int? = null

    // var missing_chapters: String? = null

    public var followStatus: Int? = null
    public var relation: MangaDexRelation? = null

    // var maxChapterNumber: Int? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = mdUuid?.let { "/manga/$it" }

        val title = title

        val cover = cover

        val author = authors?.joinToString()

        val artist = artists?.joinToString()

        val status = status

        val genres = tagsToGenreString()

        val description = description

        return manga.copy(
            url = key ?: manga.url,
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            author = author ?: manga.author,
            artist = artist ?: manga.artist,
            status = status ?: manga.status,
            genre = genres,
            description = description ?: manga.description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(mdUuid) { stringResource(SYMR.strings.id) },
                // getItem(mdUrl) { stringResource(SYMR.strings.url) },
                getItem(cover) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(authors, { it.joinToString() }) { stringResource(SYMR.strings.author) },
                getItem(artists, { it.joinToString() }) { stringResource(SYMR.strings.artist) },
                getItem(langFlag) { stringResource(SYMR.strings.language) },
                getItem(lastChapterNumber) { stringResource(SYMR.strings.last_chapter_number) },
                getItem(rating) { stringResource(SYMR.strings.average_rating) },
                // getItem(users) { stringResource(SYMR.strings.total_ratings) },
                getItem(status) { stringResource(MR.strings.status) },
                // getItem(missing_chapters) { stringResource(SYMR.strings.missing_chapters) },
                getItem(followStatus) { stringResource(SYMR.strings.follow_status) },
                getItem(anilistId) { stringResource(SYMR.strings.anilist_id) },
                getItem(kitsuId) { stringResource(SYMR.strings.kitsu_id) },
                getItem(myAnimeListId) { stringResource(SYMR.strings.mal_id) },
                getItem(mangaUpdatesId) { stringResource(SYMR.strings.manga_updates_id) },
                getItem(animePlanetId) { stringResource(SYMR.strings.anime_planet_id) },
            )
        }
    }

    public companion object {
        private const val TITLE_TYPE_MAIN = 0

        public const val TAG_TYPE_DEFAULT: Int = 0
    }
}
