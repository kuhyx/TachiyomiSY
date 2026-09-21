package eu.kanade.tachiyomi.data.track

import android.app.Application
import androidx.annotation.CallSuper
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import logcat.LogPriority
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

internal abstract class BaseTracker(
    override val id: Long,
    override val name: String,
) : Tracker {

    val trackPreferences: TrackPreferences by injectLazy()
    val networkService: NetworkHelper by injectLazy()
    private val addTracks: AddTracks by injectLazy()
    private val insertTrack: InsertTrack by injectLazy()

    override val client: OkHttpClient
        get() = networkService.client

    // Application and remote support for reading dates
    override val supportsReadingDates: Boolean = false

    override val supportsPrivateTracking: Boolean = false

    override val isLoggedIn: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    override val isLoggedInFlow: Flow<Boolean> by lazy {
        combine(
            trackPreferences.trackUsername(this).changes(),
            trackPreferences.trackPassword(this).changes(),
        ) { username, password ->
            username.isNotEmpty() && password.isNotEmpty()
        }
    }

    // Follow-up: Store all scores as 10 point in the future maybe? (https://github.com/kuhyx/TachiyomiSY/issues/17)
    override fun get10PointScore(track: DomainTrack): Double = track.score

    override fun indexToScore(index: Int): Double = index.toDouble()

    @CallSuper
    override fun logout() {
        trackPreferences.setCredentials(this, "", "")
    }

    override fun getUsername() = trackPreferences.trackUsername(this).get()

    override fun getDisplayUsername(): String = trackPreferences.trackDisplayUsername(this).get()

    override fun saveDisplayUsername(displayName: String) = trackPreferences.trackDisplayUsername(this).set(displayName)

    override fun getPassword() = trackPreferences.trackPassword(this).get()

    override fun saveCredentials(username: String, password: String) {
        trackPreferences.setCredentials(this, username, password)
    }

    override suspend fun register(item: Track, mangaId: Long) {
        item.mangaId = mangaId
        try {
            addTracks.bind(this, item, mangaId)
        } catch (expected: Throwable) {
            // Any failure ends here and the fallback below applies.
            withUIContext { Injekt.get<Application>().toast(expected.message) }
        }
    }

    override suspend fun setRemoteStatus(track: Track, status: Long) {
        track.status = status
        if (track.status == getCompletionStatus() && track.totalChapters != 0L) {
            track.lastChapterRead = track.totalChapters.toDouble()
        }
        updateRemote(track)
    }

    override suspend fun setRemoteLastChapterRead(track: Track, chapterNumber: Int) {
        if (
            track.lastChapterRead == 0.0 &&
            track.lastChapterRead < chapterNumber &&
            track.status != getRereadingStatus()
        ) {
            track.status = getReadingStatus()
        }
        track.lastChapterRead = chapterNumber.toDouble()
        if (track.totalChapters != 0L && track.lastChapterRead.toLong() == track.totalChapters) {
            track.status = getCompletionStatus()
            track.finishedReadingDate = System.currentTimeMillis()
        }
        updateRemote(track)
    }

    override suspend fun setRemoteScore(track: Track, scoreString: String) {
        track.score = indexToScore(getScoreList().indexOf(scoreString))
        updateRemote(track)
    }

    override suspend fun setRemoteStartDate(track: Track, epochMillis: Long) {
        track.startedReadingDate = epochMillis
        updateRemote(track)
    }

    override suspend fun setRemoteFinishDate(track: Track, epochMillis: Long) {
        track.finishedReadingDate = epochMillis
        updateRemote(track)
    }

    override suspend fun setRemotePrivate(track: Track, private: Boolean) {
        track.private = private
        updateRemote(track)
    }

    // SY -->
    // Trackers without a metadata endpoint simply report nothing; callers treat null as "no data".
    override suspend fun getMangaMetadata(track: DomainTrack): TrackMangaMetadata? = null

    override suspend fun searchById(id: String): TrackSearch? = null
    // SY <--

    private suspend fun updateRemote(track: Track) = withIOContext {
        try {
            update(track)
            // Never null: an absent id is substituted when it is not required.
            insertTrack.await(track.toDomainTrack(idRequired = false)!!)
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected) { "Failed to update remote track data id=$id" }
            withUIContext { Injekt.get<Application>().toast(expected.message) }
        }
    }
}
