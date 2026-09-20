package eu.kanade.tachiyomi.data.track.kavita

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.sourceIdOf
import eu.kanade.tachiyomi.source.sourcePreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

// The Kavita extension ships three sources, kavita_1 to kavita_3.
private const val KAVITA_SOURCES = 3

internal class Kavita(id: Long) : BaseTracker(id, "Kavita"), EnhancedTracker {

    var authentications: OAuth? = null

    private val interceptor by lazy { KavitaInterceptor(this) }
    val api by lazy { KavitaApi(client, interceptor) }

    private val sourceManager: SourceManager by injectLazy()

    override fun getLogo(): Int = R.drawable.brand_kavita

    override fun getStatusList(): List<Long> = listOf(UNREAD, READING, COMPLETED)

    override fun getStatus(status: Long): StringResource? = when (status) {
        UNREAD -> MR.strings.unread
        READING -> MR.strings.reading
        COMPLETED -> MR.strings.completed
        else -> null
    }

    override fun getReadingStatus(): Long = READING

    override fun getRereadingStatus(): Long = -1

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): List<String> = listOf()

    override fun displayScore(track: DomainTrack): String = ""

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        if (track.status != COMPLETED && didReadChapter) {
            if (track.lastChapterRead.toLong() == track.totalChapters && track.totalChapters > 0) {
                track.status = COMPLETED
            } else {
                track.status = READING
            }
        }
        return api.updateProgress(track)
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track = track

    // Enhanced trackers bind by URL, so the search UI is never offered for them.
    override suspend fun search(query: String): List<TrackSearch> =
        throw UnsupportedOperationException("Search is not supported by this tracker")

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.getTrackSearch(track.trackingUrl)
        track.copyPersonalFrom(remoteTrack)
        track.totalChapters = remoteTrack.totalChapters
        return track
    }

    override suspend fun login(username: String, password: String) {
        saveCredentials("user", "pass")
    }

    // [Tracker].isLogged works by checking that credentials are saved.
    // By saving dummy, unused credentials, we can activate the tracker simply by login/logout
    override fun loginNoop() {
        saveCredentials("user", "pass")
    }

    override fun getAcceptedSources() = listOf("eu.kanade.tachiyomi.extension.all.kavita.Kavita")

    override suspend fun match(manga: Manga): TrackSearch? =
        try {
            api.getTrackSearch(manga.url)
        } catch (_: Exception) {
            // Any failure ends here and the fallback below applies.
            null
        }

    override fun isTrackFrom(track: DomainTrack, manga: Manga, source: Source?): Boolean =
        track.remoteUrl == manga.url && source?.let { accept(it) } == true

    override fun migrateTrack(track: DomainTrack, manga: Manga, newSource: Source): DomainTrack? =
        if (accept(newSource)) {
            track.copy(remoteUrl = manga.url)
        } else {
            null
        }

    fun loadOAuth() {
        authentications = OAuth((1..KAVITA_SOURCES).map(::authenticate))
    }

    // The auth for one Kavita source: unconfigured or unreachable sources keep an empty token.
    private fun authenticate(id: Int): SourceAuth {
        val sourceId = sourceIdOf(name = "kavita_$id", lang = "all", versionId = 1)
        val preferences = (sourceManager.get(sourceId) as ConfigurableSource).sourcePreferences()

        val prefApiUrl = preferences.getString("APIURL", "").orEmpty()
        val prefApiKey = preferences.getString("APIKEY", "").orEmpty()
        val token = if (prefApiUrl.isEmpty() || prefApiKey.isEmpty()) {
            null
        } else {
            api.getNewToken(apiUrl = prefApiUrl, apiKey = prefApiKey)
        }
        return if (token.isNullOrEmpty()) SourceAuth(sourceId = id) else SourceAuth(id, prefApiUrl, token)
    }

    companion object {
        const val UNREAD = 1L
        const val READING = 2L
        const val COMPLETED = 3L
    }
}
