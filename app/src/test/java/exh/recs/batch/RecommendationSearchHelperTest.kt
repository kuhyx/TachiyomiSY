package exh.recs.batch

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import exh.recs.sources.sourceManga
import exh.recs.sources.track
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecommendationSearchHelperTest {
    private val f = RecSearchFixture()

    @Before
    fun setUp() = f.install()

    @After
    fun tearDown() = f.uninstall()

    @Test
    fun idleUntilStarted() {
        f.helper.status.value shouldBe SearchStatus.Idle
        f.helper.context shouldBe f.harness.application
    }

    @Test
    fun secondRunIsRejected() {
        f.helper.status.value = SearchStatus.Initializing
        f.helper.runSearch(CoroutineScope(Dispatchers.IO), emptyList()) shouldBe null
        f.helper.status.value = SearchStatus.Idle
    }

    @Test
    fun rankedResultsAreCounted() {
        f.server.body = MAL_RECS
        f.tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST))
        val status = f.runToCompletion(sourceManga(id = 1L), sourceManga(id = 2L, title = "Other"))
        val results = (status as SearchStatus.Finished.WithResults).results
        results.size shouldBe 1
        results.single().recSourceName shouldBe "MyAnimeList"
        results.single().results.values shouldContainExactly listOf(2)
    }

    @Test
    fun withoutResultsOnFailure() {
        f.server.body = "not json"
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        // An empty answer is a "no results" per source, which is expected rather than an error.
        f.server.body = """{"data":[]}"""
        f.tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST))
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
    }

    @Test
    fun trackerFlagFiltersSources() {
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES)
        f.server.body = MAL_RECS
        f.runToCompletion(sourceManga()) shouldBe SearchStatus.Finished.WithoutResults
        f.server.requests.isEmpty() shouldBe true
    }

    @Test
    fun trackedResultsAreHidden() {
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS)
        f.server.body = MAL_RECS
        f.tracks = listOf(track(trackerId = TrackerManager.MYANIMELIST, remoteUrl = "https://mal/1"))
        f.library += libraryEntryOf(title = "Another")
        val status = f.runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single().results.isEmpty() shouldBe true
    }

    @Test
    fun sourceResultsAreHidden() {
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES or SearchFlags.HIDE_LIBRARY_RESULTS)
        f.server.body = """{"comic":{"recommendations":[""" +
            """{"relates":{"title":"Rec","hid":"h","md_covers":[{"b2key":"c"}]}}]}}"""
        every { f.comickSource.name } returns "Comick"
        f.library += libraryEntryOf(title = "Rec")
        val status = f.runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        status.results.single().recSourceName shouldBe "Comick"
        status.results.single().results.isEmpty() shouldBe true
        f.preferences.recommendationSearchFlags.set(SearchFlags.INCLUDE_SOURCES)
        val kept = f.runToCompletion(sourceManga()) as SearchStatus.Finished.WithResults
        kept.results.single().results.size shouldBe 1
    }

    @Test
    fun cancellationLeavesNoStatus() {
        f.server.body = MAL_RECS
        val job = checkNotNull(f.start(sourceManga()))
        job.cancel()
        runBlocking { withTimeout(WAIT_MS) { job.join() } }
        (f.helper.status.value is SearchStatus.Finished) shouldBe false
    }

    @Test
    fun failureBecomesError() {
        every { f.sourceManager.getOrStub(any()) } throws IllegalStateException("db down")
        val status = f.runToCompletion(sourceManga()) as SearchStatus.Error
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
