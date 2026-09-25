package exh.recs.batch

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.pref.DelegateSourcePreferences
import exh.recs.sources.sourceManga
import exh.recs.sources.track
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

private const val WAIT_MS = 45_000L

private fun libraryEntryOf(title: String): LibraryManga = LibraryManga(
    manga = sourceManga(id = 5L, title = title),
    categories = listOf(0L),
    totalChapters = 0,
    readCount = 0,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)
private const val MAL_RECS = """{"data":[{"entry":{"title":"Rec One","url":"https://mal/1"}}]}"""

@RunWith(RobolectricTestRunner::class)
internal class RecommendationSearchHelperTest {
    private val harness = SourceTestHarness()
    private val server = CannedServer()
    private val preferences = SourcePreferences(harness.store)
    private val library = mutableListOf<LibraryManga>()
    private var tracks = emptyList<Track>()
    private val comickSource = mockk<Source>()
    private val getLibrary = mockk<GetLibraryManga>()
    private val sourceManager = mockk<SourceManager>()
    private lateinit var helper: RecommendationSearchHelper

    @Before
    fun setUp() {
        harness.install()
        harness.serve(preferences)
        harness.serve(DelegateSourcePreferences(harness.store))
        harness.serve<eu.kanade.tachiyomi.network.NetworkHelper>(
            eu.kanade.tachiyomi.source.online.networkHelperOf(server.client),
        )
        harness.serve(eu.kanade.domain.track.service.TrackPreferences(harness.store))
        harness.serve(TrackerManager())
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
        helper = RecommendationSearchHelper(harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    private fun start(vararg manga: Manga): Job? {
        // A finished search has to be acknowledged before the next one is accepted.
        helper.status.value = SearchStatus.Idle
        return helper.runSearch(CoroutineScope(Dispatchers.IO), manga.toList())
    }

    // Runs a search to the end, including the lock release in its `finally`.
    private fun runToCompletion(vararg manga: Manga): SearchStatus {
        val job = checkNotNull(start(*manga))
        return runBlocking {
            withTimeout(WAIT_MS) {
                val status = helper.status.first { it is SearchStatus.Finished || it is SearchStatus.Error }
                job.join()
                status
            }
        }
    }

    @Test
    fun idleUntilStarted() {
        helper.status.value shouldBe SearchStatus.Idle
        helper.context shouldBe harness.application
    }

    @Test
    fun secondRunIsRejected() {
        helper.status.value = SearchStatus.Initializing
        helper.runSearch(CoroutineScope(Dispatchers.IO), emptyList()) shouldBe null
        helper.status.value = SearchStatus.Idle
    }

    @Test
    fun rankedResultsAreCounted() {
        server.body = MAL_RECS
        tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST))
        val status = runToCompletion(sourceManga(id = 1L), sourceManga(id = 2L, title = "Other"))
        val results = (status as SearchStatus.Finished.WithResults).results
        results.size shouldBe 1
        results.single().recSourceName shouldBe "MyAnimeList"
        results.single().results.values shouldContainExactly listOf(2)
    }

    @Test
    fun withoutResultsOnFailure() {
        server.body = "not json"
        runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        // An empty answer is a "no results" per source, which is expected rather than an error.
        server.body = """{"data":[]}"""
        tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST))
        runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
    }

    @Test
    fun trackerFlagFiltersSources() {
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES)
        server.body = MAL_RECS
        runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        server.requests.isEmpty() shouldBe true
    }

    @Test
    fun trackedResultsAreHidden() {
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS)
        server.body = MAL_RECS
        tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST, remoteUrl = "https://mal/1"))
        library += libraryEntryOf(title = "Another")
        val status = runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single().results.isEmpty() shouldBe true
    }

    @Test
    fun sourceResultsAreHidden() {
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES or SearchFlags.HIDE_LIBRARY_RESULTS)
        server.body = """{"comic":{"recommendations":[""" +
            """{"relates":{"title":"Rec","hid":"h","md_covers":[{"b2key":"c"}]}}]}}"""
        every { comickSource.name } returns "Comick"
        library += libraryEntryOf(title = "Rec")
        val status = runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single().recSourceName shouldBe "Comick"
        status.results.single().results.isEmpty() shouldBe true
        preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES)
        val kept = runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        kept.results.single().results.size shouldBe 1
    }

    @Test
    fun cancellationLeavesNoStatus() {
        server.body = MAL_RECS
        val job = checkNotNull(start(sourceManga()))
        job.cancel()
        runBlocking { withTimeout(WAIT_MS) { job.join() } }
        (helper.status.value is SearchStatus.Finished) shouldBe false
    }

    @Test
    fun failureBecomesError() {
        every { sourceManager.getOrStub(any()) } throws IllegalStateException("db down")
        val status = runToCompletion(sourceManga()) as SearchStatus.Error
        status.message shouldBe "db down"
    }

    @Test
    fun statusModels() {
        SearchStatus.Processing(SManga(url = "/u", title = "t"), 1, 2).copy(current = 2).current shouldBe 2
        SearchStatus.Error("boom").copy(message = "other").message shouldBe "other"
        SearchStatus.Cancelling.toString().isNotBlank() shouldBe true
        SearchStatus.Finished.WithResults(emptyList()).copy(results = emptyList()).results.isEmpty() shouldBe true
        RecommendationSearchProgressProperties(title = "t", text = "x").copy(text = "y").text shouldBe "y"
    }
}
