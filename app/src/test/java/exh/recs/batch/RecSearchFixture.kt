package exh.recs.batch

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.networkHelperOf
import exh.pref.DelegateSourcePreferences
import exh.recs.sources.sourceManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

internal const val WAIT_MS = 45_000L
internal const val MAL_RECS = """{"data":[{"entry":{"title":"Rec One","url":"https://mal/1"}}]}"""

internal fun libraryEntryOf(title: String): LibraryManga = LibraryManga(
    manga = sourceManga(id = 5L, title = title),
    categories = listOf(0L),
    totalChapters = 0,
    readCount = 0,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)

/** The Koin graph, canned network and library a [RecommendationSearchHelper] runs against. */
internal class RecSearchFixture {
    val harness = SourceTestHarness()
    val server = CannedServer()
    val preferences = SourcePreferences(harness.store)
    val library = mutableListOf<LibraryManga>()
    var tracks = emptyList<Track>()
    val comickSource = mockk<Source>()
    val sourceManager = mockk<SourceManager>()
    lateinit var helper: RecommendationSearchHelper

    fun install(context: Context = harness.application) {
        harness.install()
        harness.serve(preferences)
        harness.serve(DelegateSourcePreferences(harness.store))
        harness.serve<NetworkHelper>(networkHelperOf(server.client))
        harness.serve(TrackPreferences(harness.store))
        harness.serve(TrackerManager())
        val getLibrary = mockk<GetLibraryManga>()
        coEvery { getLibrary.await() } answers { library }
        harness.serve(getLibrary)
        val getTracks = mockk<GetTracks>()
        coEvery { getTracks.await() } answers { tracks }
        coEvery { getTracks.await(any<Long>()) } answers { tracks }
        harness.serve(getTracks)
        // Resolving a source recommendation returns the library row of the same title, as the database would.
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers {
            val incoming = firstArg<Manga>()
            library.firstOrNull { it.manga.ogTitle == incoming.ogTitle }?.manga ?: incoming
        }
        harness.serve(networkToLocal)
        every { comickSource.id } returns 7L
        every { comickSource.name } returns "Plain"
        every { sourceManager.getOrStub(any()) } returns comickSource
        harness.serve(sourceManager)
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_TRACKERS)
        helper = RecommendationSearchHelper(context)
    }

    fun uninstall() = harness.uninstall()

    fun start(vararg manga: Manga, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): Job? {
        // A finished search has to be acknowledged before the next one is accepted.
        helper.status.value = SearchStatus.Idle
        return helper.runSearch(scope, manga.toList())
    }

    // Runs a search to the end, including the lock release in its `finally`.
    fun runToCompletion(vararg manga: Manga): SearchStatus {
        val job = checkNotNull(start(*manga))
        return runBlocking {
            withTimeout(WAIT_MS) {
                val status = helper.status.first { it is SearchStatus.Finished || it is SearchStatus.Error }
                job.join()
                status
            }
        }
    }
}
