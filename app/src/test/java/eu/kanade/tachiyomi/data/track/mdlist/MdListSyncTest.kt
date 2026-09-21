package eu.kanade.tachiyomi.data.track.mdlist

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import exh.md.utils.FollowStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [MdList.update], [MdList.bind] and [MdList.refresh], and every path that needs a MangaDex. */
internal class MdListSyncTest {

    private val harness = MdListHarness()
    private val tracker: MdList
        get() = harness.tracker

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun track(status: Long, score: Double = 0.0): Track =
        dbTrack(trackerId = 60L, status = status, score = score, trackingUrl = URL)

    private fun remote(status: Long, score: Double = 0.0): Track =
        dbTrack(trackerId = 60L, status = status, score = score, lastChapterRead = 3.0)

    @Test
    fun updatePushesANewFollowStatus() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } returns remote(FollowStatus.UNFOLLOWED.long)
        coEvery { harness.mangaDex.updateFollowStatus("uuid-1", FollowStatus.READING) } returns true
        val track = track(FollowStatus.READING.long)
        tracker.update(track) shouldBe track
        track.status shouldBe FollowStatus.READING.long
        coVerify(exactly = 0) { harness.mangaDex.updateRating(any()) }
    }

    @Test
    fun rejectedFollowStatusIsReverted() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } returns remote(FollowStatus.DROPPED.long)
        coEvery { harness.mangaDex.updateFollowStatus("uuid-1", FollowStatus.READING) } returns false
        val track = track(FollowStatus.READING.long)
        tracker.update(track, didReadChapter = true)
        track.status shouldBe FollowStatus.DROPPED.long
    }

    @Test
    fun changedScoreIsRated() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } returns remote(FollowStatus.READING.long, score = 5.0)
        coEvery { harness.mangaDex.updateRating(any()) } returns true
        val track = track(FollowStatus.READING.long, score = 8.0)
        tracker.update(track)
        coVerify(exactly = 1) { harness.mangaDex.updateRating(track) }
        coVerify(exactly = 0) { harness.mangaDex.updateFollowStatus(any(), any()) }
    }

    @Test
    fun refreshCopiesTheRemote() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } returns remote(FollowStatus.ON_HOLD.long, score = 6.0)
        val track = track(FollowStatus.READING.long)
        tracker.refresh(track) shouldBe track
        track.status shouldBe FollowStatus.ON_HOLD.long
        track.score shouldBe 6.0
        track.lastChapterRead shouldBe 3.0
    }

    @Test
    fun bindStartsUnfollowedEntries() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } answers { remote(FollowStatus.UNFOLLOWED.long) }
        coEvery { harness.mangaDex.updateFollowStatus("uuid-1", any()) } returns true
        val read = track(FollowStatus.DROPPED.long)
        tracker.bind(read, hasReadChapters = true).status shouldBe FollowStatus.READING.long
        coVerify(exactly = 1) { harness.mangaDex.updateFollowStatus("uuid-1", FollowStatus.READING) }

        val planned = track(FollowStatus.DROPPED.long)
        tracker.bind(planned).status shouldBe FollowStatus.PLAN_TO_READ.long
        coVerify(exactly = 1) { harness.mangaDex.updateFollowStatus("uuid-1", FollowStatus.PLAN_TO_READ) }
    }

    @Test
    fun bindKeepsAFollowedEntry() = runTest {
        coEvery { harness.mangaDex.fetchTrackingInfo(URL) } returns remote(FollowStatus.COMPLETED.long)
        val track = track(FollowStatus.DROPPED.long)
        tracker.bind(track, hasReadChapters = true).status shouldBe FollowStatus.COMPLETED.long
        coVerify(exactly = 0) { harness.mangaDex.updateFollowStatus(any(), any()) }
    }

    @Test
    fun everythingNeedsAMangaDex() = runTest {
        every { harness.koin.sourceManager.getVisibleOnlineSources() } returns emptyList()
        val none = MdList(60L)
        val track = track(FollowStatus.READING.long)
        shouldThrow<MdList.MangaDexNotFoundException> { none.update(track) }.message shouldBe
            "Mangadex not enabled"
        shouldThrow<MdList.MangaDexNotFoundException> { none.refresh(track) }
        shouldThrow<MdList.MangaDexNotFoundException> { none.search("q") }
        shouldThrow<MdList.MangaDexNotFoundException> { none.getMangaMetadata(domainTrack()) }
    }

    private companion object {
        const val URL = "https://mangadex.org/manga/uuid-1"
    }
}
