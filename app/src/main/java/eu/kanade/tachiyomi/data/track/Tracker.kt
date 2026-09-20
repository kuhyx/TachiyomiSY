package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import tachiyomi.domain.track.model.Track as DomainTrack

/**
 * A tracking service. Identity and feature flags live here; the rest of the contract is split by
 * capability so each piece stays small enough to read at once.
 */
internal interface Tracker : TrackerCredentials, TrackerScale, TrackerSync, TrackerRemoteFields {
    val id: Long

    val name: String

    val client: OkHttpClient

    // Application and remote support for reading dates
    val supportsReadingDates: Boolean

    val supportsPrivateTracking: Boolean

    @DrawableRes
    fun getLogo(): Int
}

// Login state and stored account details.
internal interface TrackerCredentials {
    val isLoggedIn: Boolean

    val isLoggedInFlow: Flow<Boolean>

    suspend fun login(username: String, password: String)

    @CallSuper
    fun logout()

    fun getUsername(): String

    fun getPassword(): String

    fun getDisplayUsername(): String

    fun saveDisplayUsername(displayName: String)

    fun saveCredentials(username: String, password: String)
}

// The status and score vocabularies of the service.
internal interface TrackerScale {
    fun getStatusList(): List<Long>

    fun getStatus(status: Long): StringResource?

    fun getReadingStatus(): Long

    fun getRereadingStatus(): Long

    fun getCompletionStatus(): Long

    fun getScoreList(): List<String>

    // Follow-up: Store all scores as 10 point in the future maybe? (https://github.com/kuhyx/TachiyomiSY/issues/17)
    fun get10PointScore(track: DomainTrack): Double

    fun indexToScore(index: Int): Double

    fun displayScore(track: DomainTrack): String
}

// Two-way exchange of whole track entries with the service.
internal interface TrackerSync {
    suspend fun update(track: Track, didReadChapter: Boolean = false): Track

    suspend fun bind(track: Track, hasReadChapters: Boolean = false): Track

    suspend fun search(query: String): List<TrackSearch>

    suspend fun refresh(track: Track): Track

    // Follow-up: move this to an interactor, and update all trackers based on common data
    // https://github.com/kuhyx/TachiyomiSY/issues/6
    suspend fun register(item: Track, mangaId: Long)

    // SY -->
    suspend fun getMangaMetadata(track: DomainTrack): TrackMangaMetadata?

    suspend fun searchById(id: String): TrackSearch?
    // SY <--
}

// Single-field pushes to the remote entry.
internal interface TrackerRemoteFields {
    suspend fun setRemoteStatus(track: Track, status: Long)

    suspend fun setRemoteLastChapterRead(track: Track, chapterNumber: Int)

    suspend fun setRemoteScore(track: Track, scoreString: String)

    suspend fun setRemoteStartDate(track: Track, epochMillis: Long)

    suspend fun setRemoteFinishDate(track: Track, epochMillis: Long)

    suspend fun setRemotePrivate(track: Track, private: Boolean)
}
