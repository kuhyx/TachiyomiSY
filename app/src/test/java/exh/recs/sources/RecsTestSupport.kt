package exh.recs.sources

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import eu.kanade.tachiyomi.source.online.networkHelperOf
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.OkHttpClient
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

/** A tracked entry of [trackerId] pointing at [remoteId] / [remoteUrl]. */
internal fun track(trackerId: Long, remoteId: Long = 42L, remoteUrl: String = "https://tracker/42"): Track = Track(
    id = 1L,
    mangaId = 1L,
    trackerId = trackerId,
    remoteId = remoteId,
    libraryId = null,
    title = "t",
    lastChapterRead = 0.0,
    totalChapters = 0,
    status = 0,
    score = 0.0,
    remoteUrl = remoteUrl,
    startDate = 0,
    finishDate = 0,
    private = false,
)

/** A manga in the library, with the title the recommendation sources search by. */
internal fun sourceManga(id: Long = 1L, title: String = "Source Title"): Manga =
    Manga.create().copy(id = id, url = "/comic/hid#", source = 7L, ogTitle = title)

/**
 * The Injekt graph a recommendation source pulls: preferences, the tracker ids, the shared client
 * (pointed at [client]) and [tracks] for the manga under test.
 */
internal class RecsStub(client: OkHttpClient, tracks: List<Track> = emptyList()) {
    private val stub = InjektStub()
    val trackerManager: TrackerManager

    /** The tracks the served [GetTracks] answers with; assign to retrack the manga under test. */
    var tracks: List<Track> = tracks

    init {
        stub.install()
        stub.serve(TrackPreferences(MemoPreferenceStore()))
        val getTracks = mockk<GetTracks>()
        coEvery { getTracks.await(any<Long>()) } answers { this@RecsStub.tracks }
        stub.serve(getTracks)
        stub.serve<NetworkHelper>(networkHelperOf(client))
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        stub.serve(networkToLocal)
        trackerManager = TrackerManager()
        stub.serve(trackerManager)
    }

    /** Registers [instance] for [T] in the served graph. */
    inline fun <reified T : Any> serve(instance: T) = stub.serve(instance)

    fun uninstall() = stub.uninstall()
}
