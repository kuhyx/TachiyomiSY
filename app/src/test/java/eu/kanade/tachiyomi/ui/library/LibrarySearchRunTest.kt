package eu.kanade.tachiyomi.ui.library

import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState

@RunWith(RobolectricTestRunner::class)
internal class LibrarySearchRunTest {
    private val parts = LibrarySearchParts()
    private val items = listOf(
        libItem(libEntry(libManga(1L, title = "Needle"))),
        libItem(libEntry(libManga(2L, title = "Haystack", source = EH_SOURCE_ID))),
        libItem(libEntry(libManga(3L, title = "Pin", source = EH_SOURCE_ID))),
    )

    @Before
    fun setUp() {
        parts.start()
        coEvery { parts.getIds.await() } returns listOf(2L)
        coEvery { parts.getSearchTags.await(2L) } returns listOf(searchTag("misc", "needle"))
        coEvery { parts.getSearchTitles.await(2L) } returns emptyList()
        coEvery { parts.getTracks.await() } returns listOf(track(mangaId = 3L, trackerId = 5L))
    }

    @After
    fun tearDown() = parts.stop()

    private fun search(query: String?, loggedIn: Map<Long, TriState> = emptyMap()): List<Long> =
        runBlocking { parts.search.filterLibrary(items, query, loggedIn) }.map { it.id }

    @Test
    fun nothingToFilter() {
        runBlocking { parts.search.filterLibrary(emptyList(), "x", emptyMap()) }.shouldBeEmpty()
        search(null) shouldContainExactly listOf(1L, 2L, 3L)
        search("  ") shouldContainExactly listOf(1L, 2L, 3L)
    }

    @Test
    fun idQueryPicksOneEntry() {
        search("id:2") shouldContainExactly listOf(2L)
        search("ID:2").shouldBeEmpty()
    }

    @Test
    fun metadataEntriesSearchTheirTags() {
        // Entry 2 has metadata and matches through its tags; entry 3 is an EH entry without metadata.
        search("needle") shouldContainExactly listOf(1L, 2L)
        coVerify(exactly = 0) { parts.getTracks.await() }
        coVerify(exactly = 0) { parts.getSearchTags.await(3L) }
    }

    @Test
    fun loggedInTrackersSearchTracks() {
        search("trk", parts.loggedIn) shouldContainExactly listOf(3L)
        coVerify { parts.getTracks.await() }
    }
}
