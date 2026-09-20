package eu.kanade.tachiyomi.data.track.mdlist

import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import exh.md.network.MangaDexAuthInterceptor
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.domain.track.model.Track as DomainTrack

internal class MdList(id: Long) : BaseTracker(id, "MDList") {

    private val mdex by lazy { MdUtil.getEnabledMangaDex() }

    val interceptor = MangaDexAuthInterceptor(trackPreferences, this)

    override val isLoggedIn: Boolean
        get() = trackPreferences.trackToken(this).get().isNotEmpty()

    override fun getLogo(): Int = R.drawable.brand_mangadex

    override fun getStatusList(): List<Long> = FollowStatus.entries.map { it.long }

    override fun getStatus(status: Long): StringResource? =
        STATUS_LABELS[FollowStatus.entries.firstOrNull { it.long == status }]

    override fun getScoreList() = SCORE_LIST

    override fun displayScore(track: DomainTrack) = track.score.toInt().toString()

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()

            val remoteTrack = mdex.fetchTrackingInfo(track.trackingUrl)
            val followStatus = FollowStatus.fromLong(track.status)

            // this updates the follow status in the metadata
            // allow follow status to update
            if (remoteTrack.status != followStatus.long) {
                if (mdex.updateFollowStatus(MdUtil.getMangaId(track.trackingUrl), followStatus)) {
                    remoteTrack.status = followStatus.long
                } else {
                    track.status = remoteTrack.status
                }
            }

            if (remoteTrack.score != track.score) {
                mdex.updateRating(track)
            }

            // mangadex wont update chapters if manga is not follows this prevents unneeded network call

            /*if (followStatus != FollowStatus.UNFOLLOWED) {
                if (track.totalChapters != 0 && track.lastChapterRead == track.totalChapters) {
                    track.status = FollowStatus.COMPLETED.int
                    mdex.updateFollowStatus(MdUtil.getMangaId(track.trackingUrl), FollowStatus.COMPLETED)
                }
                if (followStatus == FollowStatus.PLAN_TO_READ && track.lastChapterRead > 0) {
                    val newFollowStatus = FollowStatus.READING
                    track.status = FollowStatus.READING.int
                    mdex.updateFollowStatus(MdUtil.getMangaId(track.trackingUrl), newFollowStatus)
                    remoteTrack.status = newFollowStatus.int
                }

                mdex.updateReadingProgress(track)
            } else if (track.lastChapterRead != 0) {
                // When followStatus has been changed to unfollowed 0 out read chapters since dex does
                track.lastChapterRead = 0
            }*/
            track
        }
    }

    override fun getCompletionStatus(): Long = FollowStatus.COMPLETED.long

    override fun getReadingStatus(): Long = FollowStatus.READING.long

    override fun getRereadingStatus(): Long = FollowStatus.RE_READING.long

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track = update(
        refresh(track).also {
            if (it.status == FollowStatus.UNFOLLOWED.long) {
                it.status = if (hasReadChapters) {
                    FollowStatus.READING.long
                } else {
                    FollowStatus.PLAN_TO_READ.long
                }
            }
        },
    )

    override suspend fun refresh(track: Track): Track {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            val remoteTrack = mdex.fetchTrackingInfo(track.trackingUrl)
            track.copyPersonalFrom(remoteTrack)
            /*if (track.totalChapters == 0 && mangaMetadata.status == SManga.COMPLETED) {
                track.totalChapters = mangaMetadata.maxChapterNumber ?: 0
            }*/
            track
        }
    }

    fun createInitialTracker(dbManga: Manga, mdManga: Manga = dbManga): Track {
        return Track.create(id).apply {
            mangaId = dbManga.id
            status = FollowStatus.UNFOLLOWED.long
            trackingUrl = MdUtil.baseUrl + mdManga.url
            title = mdManga.title
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            mdex.getSearchManga(1, query, FilterList())
                .mangas
                .map {
                    toTrackSearch(mdex.getMangaDetails(it))
                }
                .distinct()
        }
    }

    private fun toTrackSearch(mangaInfo: SManga): TrackSearch = TrackSearch.create(id).apply {
        trackingUrl = MdUtil.baseUrl + mangaInfo.url
        title = mangaInfo.title
        coverUrl = mangaInfo.thumbnail_url.orEmpty()
        summary = mangaInfo.description.orEmpty()
    }

    override suspend fun login(username: String, password: String) =
        throw UnsupportedOperationException("MDList signs in through the MangaDex source")

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
    }

    override suspend fun getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
        return withIOContext {
            val mdex = mdex ?: throw MangaDexNotFoundException()
            val manga = mdex.getMangaMetadata(track.toDbTrack())
            TrackMangaMetadata(
                remoteId = 0,
                title = manga.title,
                thumbnailUrl = manga.thumbnail_url, // Doesn't load the actual cover because of Refer header
                description = manga.description,
                authors = manga.author,
                artists = manga.artist,
            )
        }
    }

    class MangaDexNotFoundException : Exception("Mangadex not enabled")

    companion object {
        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
    }
}

private val STATUS_LABELS = mapOf(
    FollowStatus.UNFOLLOWED to SYMR.strings.md_follows_unfollowed,
    FollowStatus.READING to MR.strings.reading,
    FollowStatus.COMPLETED to MR.strings.completed,
    FollowStatus.ON_HOLD to MR.strings.on_hold,
    FollowStatus.PLAN_TO_READ to MR.strings.plan_to_read,
    FollowStatus.DROPPED to MR.strings.dropped,
    FollowStatus.RE_READING to MR.strings.repeating,
)
