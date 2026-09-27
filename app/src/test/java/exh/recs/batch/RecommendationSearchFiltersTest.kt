package exh.recs.batch

import exh.recs.sources.track
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Which recommendation sources the search flags let through, and which library results they hide. */
@RunWith(RobolectricTestRunner::class)
internal class RecommendationSearchFiltersTest {
    private val rig = RecsSearchRig()
    private lateinit var helper: RecommendationSearchHelper

    @Before
    fun setUp() {
        rig.sources = {
            listOf(
                FakeRecsSource("Linked", linkedTo = 7L) { pageOf("Owned title", "New title") },
                FakeTrackerSource(listOf("https://x/a", "https://x/b")),
            )
        }
        helper = rig.install()
    }

    @After
    fun tearDown() {
        unmockkAll()
        rig.uninstall()
    }

    private fun resultsWith(flags: Int): Map<String, List<String>> {
        rig.preferences.recommendationSearchFlags.set(flags)
        helper.status.value = SearchStatus.Idle
        val status = helper.finish()
        if (status == SearchStatus.Finished.WithoutResults) return emptyMap()
        return status.shouldBeInstanceOf<SearchStatus.Finished.WithResults>().results
            .associate { result -> result.recSourceName to result.results.keys.map { it.title } }
    }

    @Test
    fun noFlagsKeepNeitherKind() {
        resultsWith(0) shouldBe emptyMap()
    }

    @Test
    fun sourceFlagKeepsLinkedSources() {
        resultsWith(SearchFlags.INCLUDE_SOURCES).keys shouldContainExactly setOf("Linked")
    }

    @Test
    fun trackerFlagKeepsTrackers() {
        resultsWith(SearchFlags.INCLUDE_TRACKERS).keys shouldContainExactly setOf("Tracked")
    }

    @Test
    fun linkedLibraryEntriesAreHidden() {
        val results = resultsWith(SearchFlags.INCLUDE_SOURCES or SearchFlags.HIDE_LIBRARY_RESULTS)
        results shouldBe mapOf("Linked" to listOf("New title"))
    }

    @Test
    fun trackedNeedSameTracker() {
        rig.tracks = listOf(
            track(trackerId = 6L, remoteUrl = "https://x/a"),
            track(trackerId = 5L, remoteUrl = "https://x/other"),
            track(trackerId = 5L, remoteUrl = "https://x/b"),
        )
        val results = resultsWith(SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS)
        results shouldBe mapOf("Tracked" to listOf("https://x/a"))
    }
}
