package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.MergedMangaReference
import java.util.concurrent.atomic.AtomicBoolean

private const val MANGADEX = 2_499_283_573_021_220_255L

/**
 * The MdList track of a merged entry whose MangaDex member leaves the merge after the tracker
 * observer started: every track change reads the current merge, so none is created and the
 * observer keeps counting.
 */
@RunWith(RobolectricTestRunner::class)
internal class MangaTrackingMergeTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts
    private val saved = mangaDexSourceIds
    private val tracks = MutableStateFlow(listOf(domainTrack(trackerId = TrackerManager.MDLIST)))
    private val member = manga().copy(id = 5L, source = MANGADEX)
    private val other = manga().copy(id = 6L)
    private val members = MutableStateFlow(listOf(member, other))
    private val created = AtomicBoolean()
    private lateinit var logged: MutableList<String>

    @Before
    fun setUp() {
        mangaDexSourceIds = listOf(MANGADEX)
        harness.start()
        logged = captureLogcat()
        val plain = mockk<BaseTracker> { every { id } returns 1L }
        every { harness.trackerManager.loggedInTrackersFlow() } returns MutableStateFlow(listOf(plain))
        every { harness.trackerManager.mdList.isLoggedIn } returns true
        every { harness.trackerManager.mdList.id } returns TrackerManager.MDLIST
        coEvery { harness.trackerManager.mdList.createInitialTracker(any(), any()) } answers {
            created.set(true)
            dbTrack(TrackerManager.MDLIST).also { it.mangaId = 1L }
        }
        coEvery { parts.getTracks.subscribe(1L) } returns tracks
        val references = listOf(mockk<MergedMangaReference>(relaxed = true))
        coEvery { harness.getMergedReferences.await(1L) } returns references
        coEvery { harness.getMergedReferences.subscribe(1L) } returns MutableStateFlow(references)
        coEvery { harness.getMergedManga.await(1L) } returns members.value
        coEvery { harness.getMergedManga.subscribe(1L) } returns members
    }

    @After
    fun tearDown() {
        releaseLogcat()
        harness.stop()
        mangaDexSourceIds = saved
    }

    private fun load(): MangaScreenModel {
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
        return harness.loaded().also { model -> model.awaitSuccess { it.mergedData != null } }
    }

    private fun failed(): Boolean = logged.any { "Could not create initial track" in it }

    // Every later track change still reaches the count, and no MdList track is made for the gone member.
    private fun keepsCounting(model: MangaScreenModel) {
        tracks.value = listOf(domainTrack(trackerId = 1L))
        model.awaitSuccess { it.trackingCount == 1 }
        tracks.value = listOf(domainTrack(trackerId = 1L), domainTrack(id = 2L, trackerId = 1L))
        model.awaitSuccess { it.trackingCount == 2 }
        failed() shouldBe false
        created.get() shouldBe false
    }

    @Test
    fun unmergedMemberKeepsCounting() {
        val model = load()
        members.value = emptyList()
        model.awaitSuccess { it.mergedData == null }
        keepsCounting(model)
    }

    @Test
    fun nonMangaDexMergeKeepsCounting() {
        val model = load()
        members.value = listOf(other)
        model.awaitSuccess { state -> state.mergedData?.manga?.keys == setOf(6L) }
        keepsCounting(model)
    }

    @Test
    fun createdTrackIsLookedUp() {
        coEvery { parts.getTracks.await(1L) } returns listOf(
            domainTrack(trackerId = 1L),
            domainTrack(id = 7L, trackerId = TrackerManager.MDLIST),
        )
        val model = load()
        model.awaitSuccess().trackingCount shouldBe 0
        tracks.value = listOf(domainTrack(trackerId = 1L))
        // Counted only once the created track came back from the lookup.
        model.awaitSuccess { it.trackingCount == 1 }
        coVerify { parts.insertTrack.await(any()) }
        failed() shouldBe false
    }
}
