package exh.recs.batch

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SearchFlagsTest {
    @Test
    fun eachFlagIsIndependent() {
        SearchFlags.hasIncludeSources(SearchFlags.INCLUDE_SOURCES) shouldBe true
        SearchFlags.hasIncludeSources(SearchFlags.INCLUDE_TRACKERS) shouldBe false
        SearchFlags.hasIncludeTrackers(SearchFlags.INCLUDE_TRACKERS) shouldBe true
        SearchFlags.hasIncludeTrackers(SearchFlags.HIDE_LIBRARY_RESULTS) shouldBe false
        SearchFlags.hasHideLibraryResults(SearchFlags.HIDE_LIBRARY_RESULTS) shouldBe true
        SearchFlags.hasHideLibraryResults(0) shouldBe false
    }

    @Test
    fun flagsCombine() {
        val all = SearchFlags.INCLUDE_SOURCES or SearchFlags.INCLUDE_TRACKERS or SearchFlags.HIDE_LIBRARY_RESULTS
        SearchFlags.hasIncludeSources(all) shouldBe true
        SearchFlags.hasIncludeTrackers(all) shouldBe true
        SearchFlags.hasHideLibraryResults(all) shouldBe true
    }
}
