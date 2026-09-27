package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.mutableStateOf
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

internal fun domainTrack(trackerId: Long): Track = Track(
    id = trackerId,
    mangaId = 1L,
    trackerId = trackerId,
    remoteId = 10L,
    libraryId = null,
    title = "Tracked",
    lastChapterRead = 0.0,
    totalChapters = 0L,
    status = 0L,
    score = 0.0,
    remoteUrl = "",
    startDate = 0L,
    finishDate = 0L,
    private = false,
)

/** Autofilling the edit form from the manga's trackers. */
@RunWith(RobolectricTestRunner::class)
internal class EditMangaTrackerTest {
    private val rig = EditMangaRig()
    private val getTracks = mockk<GetTracks>()
    private val plain = mockk<BaseTracker>(relaxed = true) { every { name } returns "Plain" }
    private val other = mockk<BaseTracker>(relaxed = true) { every { name } returns "Other" }
    private val enhanced = mockk<BaseTracker>(relaxed = true, moreInterfaces = arrayOf(EnhancedTracker::class))
    private val trackers = mockk<TrackerManager> {
        every { get(1L) } returns plain
        every { get(2L) } returns other
        every { get(3L) } returns enhanced
        every { get(4L) } returns null
    }
    private val tracks = mutableStateOf(emptyList<Pair<Track, Tracker>>())
    private val selecting = mutableStateOf(false)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun fill(vararg ids: Long) {
        coEvery { getTracks.await(1L) } returns ids.map(::domainTrack)
        runBlocking {
            getTrackers(rig.sourced(), rig.binding, rig.context, getTracks, trackers, tracks, selecting)
        }
    }

    @Test
    fun untrackedToasts() {
        fill(3L, 4L)
        ShadowToast.getTextOfLatestToast() shouldBe "Entry is not tracked."
        selecting.value shouldBe false
    }

    @Test
    fun severalTrackersAsk() {
        fill(1L, 2L)
        selecting.value shouldBe true
        tracks.value.map { it.second } shouldBe listOf(plain, other)
    }

    @Test
    fun oneTrackerFillsTheForm() {
        coEvery { plain.getMangaMetadata(any()) } returns TrackMangaMetadata(
            title = "T",
            authors = " ",
            artists = "Ar",
            thumbnailUrl = "https://t",
            description = null,
        )
        fill(1L)
        rig.binding.title.text.toString() shouldBe "T"
        rig.binding.mangaAuthor.text.toString() shouldBe ""
        rig.binding.mangaArtist.text.toString() shouldBe "Ar"
        rig.binding.thumbnailUrl.text.toString() shouldBe "https://t"
    }

    @Test
    fun missingMetadataFillsNothing() {
        coEvery { plain.getMangaMetadata(any()) } returns null
        fill(1L)
        rig.binding.title.text.toString() shouldBe ""
    }

    @Test
    fun trackerErrorsToast() {
        coEvery { plain.getMangaMetadata(any()) } throws IllegalStateException("boom")
        runBlocking { autofillFromTracker(rig.binding, domainTrack(1L), plain) }
        ShadowToast.getTextOfLatestToast() shouldBe "Plain error: boom"
        coEvery { plain.getMangaMetadata(any()) } throws IllegalStateException()
        runBlocking { autofillFromTracker(rig.binding, domainTrack(1L), plain) }
        ShadowToast.getTextOfLatestToast() shouldBe "Plain error: "
    }
}
