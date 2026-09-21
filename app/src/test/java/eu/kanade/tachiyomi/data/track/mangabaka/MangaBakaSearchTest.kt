package eu.kanade.tachiyomi.data.track.mangabaka

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [MangaBaka.search]: an `id:` query hits the details endpoint, anything else the search one. */
@RunWith(RobolectricTestRunner::class)
internal class MangaBakaSearchTest {

    private val harness = MangaBakaHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun plainQueryUsesSearch() = runTest {
        harness.enqueue("search.json")
        harness.tracker.search("test").size shouldBe 2
        harness.takeRequest().url.encodedPath shouldBe "/v1/series/search"
    }

    @Test
    fun idQueryUsesDetails() = runTest {
        harness.enqueue("series.json")
        val results = harness.tracker.search("id:1234")
        results.size shouldBe 1
        results.single().remoteId shouldBe 1234L
        harness.takeRequest().url.encodedPath shouldBe "/v1/series/1234"

        harness.enqueueRaw("", code = 404)
        harness.tracker.search("id:5") shouldBe emptyList()
    }

    @Test
    fun malformedIdFallsBackToSearch() = runTest {
        harness.enqueue("search.json")
        harness.tracker.search("id:abc").size shouldBe 2
        harness.takeRequest().url.queryParameter("q") shouldBe "id:abc"
    }
}
