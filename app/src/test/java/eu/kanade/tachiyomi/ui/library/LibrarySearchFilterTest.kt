package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.domainTrack
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.TriState

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
internal class LibrarySearchFilterTest {
    private val harness = LibraryHarness()
    private val rig = LibrarySearchRig(harness)
    private val items by lazy {
        listOf(
            libraryItem(manga(1, "Alpha").copy(source = 5)),
            libraryItem(manga(2, "Beta").copy(source = EH_SOURCE_ID)),
            libraryItem(manga(3, "Gamma").copy(source = EH_SOURCE_ID)),
        )
    }

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules()) }
        every { harness.sourceManager.get(5L) } returns rig.source("Five")
        every { harness.sourceManager.get(EH_SOURCE_ID) } returns null
        coEvery { harness.getIdsWithMetadata.await() } returns listOf(2L)
        coEvery { harness.getSearchTags.await(2L) } returns listOf(tag("alp"))
        coEvery { harness.getSearchTitles.await(2L) } returns emptyList()
        coEvery { harness.getTracks.await() } returns emptyList()
    }

    @After
    fun tearDown() = stopKoin()

    private fun filter(query: String?, loggedIn: Map<Long, TriState> = emptyMap(), list: List<LibraryItem> = items) =
        runBlocking { rig.search.filterLibrary(list, query, loggedIn) }.map { it.id }

    @Test
    fun blankQueriesKeepEverything() {
        filter(null) shouldBe listOf(1L, 2L, 3L)
        filter(" ") shouldBe listOf(1L, 2L, 3L)
        filter("alpha", list = emptyList()) shouldBe emptyList()
    }

    @Test
    fun idQueriesPickOne() {
        filter("id:3") shouldBe listOf(3L)
        filter("id:x") shouldBe emptyList()
    }

    @Test
    fun metadataEntriesUseTheirTags() {
        filter("alp") shouldBe listOf(1L, 2L)
        filter("gam") shouldBe listOf(3L)
        coVerify(exactly = 0) { harness.getTracks.await() }
    }

    @Test
    fun loggedInTrackersLoadTracks() {
        filter("beta", loggedIn = mapOf(7L to TriState.DISABLED)) shouldBe listOf(2L)
        coVerify { harness.getTracks.await() }
    }

    @Test
    fun trackMatchingNeedsAKnownTracker() {
        val context = harness.application
        val tracks = listOf(domainTrack(trackerId = 7L), domainTrack(trackerId = 8L))
        val known = mockk<BaseTracker> {
            every { name } returns "Kitsu"
            every { getStatus(any()) } returns null
        }
        every { harness.trackerManager.get(7L) } returns known
        every { harness.trackerManager.get(8L) } returns null
        rig.search.filterTracks("kitsu", tracks, context).shouldBeTrue()
        rig.search.filterTracks("zzz", tracks, context).shouldBeFalse()
    }
}
