package eu.kanade.tachiyomi.ui.library

import exh.search.QueryComponent
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibrarySearchTextTest {
    private val parts = LibrarySearchParts()

    // One query per searchable field of the full entry, in the order the search checks them.
    private val fieldQueries = listOf("needle", "au", "ar", "de", "src", "1", "trk", "ge", "tg", "alt")

    @Before
    fun setUp() = parts.start()

    @After
    fun tearDown() = parts.stop()

    @Test
    fun everyFieldMatches() {
        fieldQueries.map { parts.fullPasses(text(it)) } shouldContainExactly List(fieldQueries.size) { true }
        parts.fullPasses(text("zzz")) shouldBe false
    }

    @Test
    fun everyFieldBlocksAnExclusion() {
        fieldQueries.map { parts.fullPasses(text(it, excluded = true)) } shouldContainExactly
            List(fieldQueries.size) { false }
        parts.fullPasses(text("zzz", excluded = true)) shouldBe true
        parts.fullPasses(text("", excluded = true)) shouldBe true
    }

    @Test
    fun bareEntriesMatchOnlyTheTitle() {
        parts.barePasses(text("bare")) shouldBe true
        parts.barePasses(text("zzz")) shouldBe false
        parts.barePasses(text("zzz"), local = true) shouldBe false
        parts.barePasses(text("zzz", excluded = true)) shouldBe true
        // A local entry has no source id to compare, which fails the exclusion.
        parts.barePasses(text("zzz", excluded = true), local = true) shouldBe false
    }

    @Test
    fun genresCanBeSkipped() {
        val passes = parts.search.filterManga(
            queries = listOf(text("ge")),
            libraryManga = parts.full,
            tracks = parts.tracks,
            source = parts.source,
            checkGenre = false,
            loggedInTrackServices = parts.loggedIn,
        )
        passes shouldBe false
    }

    @Test
    fun tracksNeedALoggedInTracker() {
        val passes = parts.search.filterManga(
            queries = listOf(text("trk")),
            libraryManga = parts.full,
            tracks = parts.tracks,
            source = parts.source,
            loggedInTrackServices = emptyMap(),
        )
        passes shouldBe false
    }

    @Test
    fun otherComponentsPass() {
        parts.barePasses(QueryComponent()) shouldBe true
    }

    @Test
    fun trackStatusAndNameMatch() {
        val search = parts.search
        search.filterTracks("read", parts.tracks, parts.app) shouldBe true
        search.filterTracks("trk", listOf(track(mangaId = 1L, trackerId = 5L, status = 3L)), parts.app) shouldBe true
        search.filterTracks("zzz", parts.tracks, parts.app) shouldBe false
        search.filterTracks("trk", listOf(track(mangaId = 1L, trackerId = 8L)), parts.app) shouldBe false
        // A tracker that disappears between the two lookups contributes no name.
        every { parts.trackerManager.get(7L) } returnsMany listOf(parts.tracker, null)
        search.filterTracks("trk", listOf(track(mangaId = 1L, trackerId = 7L)), parts.app) shouldBe false
    }
}
