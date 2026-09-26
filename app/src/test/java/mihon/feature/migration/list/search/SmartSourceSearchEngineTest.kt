package mihon.feature.migration.list.search

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

/** A manga the source answers with. */
internal fun searchHit(title: String): SManga = SManga.create().apply {
    this.title = title
    url = "/manga/$title"
}

internal class SmartSourceSearchEngineTest {
    // deepSearch runs its queries concurrently on Dispatchers.Default.
    private val queries = CopyOnWriteArrayList<String>()
    private var hits: List<SManga> = emptyList()

    private fun source(): Source = mockk<Source> {
        every { id } returns 11L
        every { getFilterList() } returns eu.kanade.tachiyomi.source.model.FilterList()
        coEvery { getSearchManga(any(), any(), any()) } answers {
            queries += secondArg<String>()
            MangasPage(mangas = hits, hasNextPage = false)
        }
    }

    @Test
    fun regularSearchTakesTheOnlyHit() = runTest {
        hits = listOf(searchHit("Totally Different"))
        val found = SmartSourceSearchEngine(extraSearchParams = null).regularSearch(source(), "Needle")
        // A single query with a single candidate skips the distance check entirely.
        found?.title shouldBe "Totally Different"
        found?.source shouldBe 11L
        queries shouldBe listOf("Needle")
    }

    @Test
    fun regularSearchRanksCandidates() = runTest {
        hits = listOf(searchHit("Nothing Alike"), searchHit("Needle"))
        val found = SmartSourceSearchEngine(extraSearchParams = "lang:en").regularSearch(source(), "Needle")
        found?.title shouldBe "Needle"
        queries shouldBe listOf("Needle lang:en")
    }

    @Test
    fun regularSearchWithoutHits() = runTest {
        hits = emptyList()
        SmartSourceSearchEngine(extraSearchParams = "  ").regularSearch(source(), "Needle").shouldBeNull()
        queries shouldBe listOf("Needle")
    }

    @Test
    fun deepSearchCleansTheTitle() = runTest {
        hits = listOf(searchHit("[Group] Bracketed Title (v2)"), searchHit("Unrelated Words Here"))
        val found = SmartSourceSearchEngine(extraSearchParams = null)
            .deepSearch(source(), "[Group] Bracketed Title (v2)")
        found?.title shouldBe "[Group] Bracketed Title (v2)"
        // Cleaned title, two largest words, largest word, first two words, first word, deduplicated.
        queries shouldContainExactlyInAnyOrder listOf("bracketed title", "bracketed")
    }

    @Test
    fun deepSearchReadsBackwards() = runTest {
        hits = listOf(searchHit("ab"))
        // Unbalanced opening bracket: read forward the title collapses, so it is parsed reversed.
        val found = SmartSourceSearchEngine(extraSearchParams = null).deepSearch(source(), "ab (сказка - глава 3")
        found?.title shouldBe "ab"
        // The chapter reference is stripped and the cyrillic words do not survive the latin filter.
        queries shouldBe listOf("ab")
    }

    @Test
    fun deepSearchBelowThreshold() = runTest {
        hits = listOf(searchHit("Nothing At All Alike"), searchHit("Second Miss"))
        SmartSourceSearchEngine(extraSearchParams = null).deepSearch(source(), "Precise Needle Title").shouldBeNull()
        queries shouldContainExactlyInAnyOrder listOf("precise needle title", "precise needle", "precise")
    }

    @Test
    fun deepSearchKeepsForeignText() = runTest {
        hits = listOf(searchHit("манга"))
        // Stripping non-latin characters would leave nothing, so the cyrillic title is kept as the query.
        SmartSourceSearchEngine(extraSearchParams = null).deepSearch(source(), "манга")?.title shouldBe "манга"
        queries shouldBe listOf("манга")
    }

    @Test
    fun thresholdConstant() {
        BaseSmartSearchEngine.MIN_ELIGIBLE_THRESHOLD shouldBe 0.4
        SearchEntry(entry = "x", distance = 1.0).copy(distance = 0.5).distance shouldBe 0.5
        SearchEntry(entry = "x", distance = 1.0).entry shouldBe "x"
    }
}
