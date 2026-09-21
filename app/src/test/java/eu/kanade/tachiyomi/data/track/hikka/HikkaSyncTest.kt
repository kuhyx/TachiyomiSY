package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.data.track.fixture
import eu.kanade.tachiyomi.data.track.hikka.dto.HKRead
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [Hikka.update], [Hikka.bind], [Hikka.refresh], [Hikka.search] and [Hikka.delete] over the mock server. */
@RunWith(RobolectricTestRunner::class)
internal class HikkaSyncTest {

    private val harness = HikkaHarness()
    private val tracker: Hikka
        get() = harness.tracker

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun track(status: Long, lastChapterRead: Double = 0.0, totalChapters: Long = 0L): Track = dbTrack(
        trackerId = 10L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
        trackingUrl = HikkaHarness.TRACKING_URL,
    )

    private fun enqueueReadLookupThenPut() {
        harness.enqueue("read_no_content.json")
        harness.enqueue("read.json")
    }

    private fun putBody(): String {
        harness.takeRequest()
        return harness.takeRequest().bodyText()
    }

    @Test
    fun updateWithoutReadKeepsStatus() = runTest {
        enqueueReadLookupThenPut()
        val paused = track(Hikka.ON_HOLD, lastChapterRead = 3.0, totalChapters = 3L)
        tracker.update(paused).trackerId shouldBe 10L
        paused.status shouldBe Hikka.ON_HOLD
        putBody() shouldContain """"status":"on_hold""""

        enqueueReadLookupThenPut()
        val done = track(Hikka.COMPLETED, lastChapterRead = 3.0, totalChapters = 3L)
        tracker.update(done, didReadChapter = true)
        done.status shouldBe Hikka.COMPLETED
        done.finishedReadingDate shouldBe 0L
    }

    @Test
    fun readingTheLastChapterCompletes() = runTest {
        enqueueReadLookupThenPut()
        val before = System.currentTimeMillis()
        val last = track(Hikka.READING, lastChapterRead = 10.0, totalChapters = 10L)
        tracker.update(last, didReadChapter = true)
        last.status shouldBe Hikka.COMPLETED
        last.finishedReadingDate shouldBeGreaterThan before - 1

        enqueueReadLookupThenPut()
        val unknownTotal = track(Hikka.PLAN_TO_READ, lastChapterRead = 0.0, totalChapters = 0L)
        tracker.update(unknownTotal, didReadChapter = true)
        unknownTotal.status shouldBe Hikka.READING
    }

    @Test
    fun readingStartsOrKeepsRereading() = runTest {
        enqueueReadLookupThenPut()
        val first = track(Hikka.PLAN_TO_READ, lastChapterRead = 1.0, totalChapters = 10L)
        tracker.update(first, didReadChapter = true)
        first.status shouldBe Hikka.READING
        first.startedReadingDate shouldBeGreaterThan 0L

        enqueueReadLookupThenPut()
        val later = track(Hikka.ON_HOLD, lastChapterRead = 5.0, totalChapters = 10L)
        tracker.update(later, didReadChapter = true)
        later.status shouldBe Hikka.READING
        later.startedReadingDate shouldBe 0L

        enqueueReadLookupThenPut()
        val again = track(Hikka.REREADING, lastChapterRead = 2.0, totalChapters = 10L)
        tracker.update(again, didReadChapter = true)
        again.status shouldBe Hikka.REREADING
    }

    @Test
    fun bindWithReadCopiesProgress() = runTest {
        harness.enqueue("read.json")
        harness.enqueue("manga.json")
        enqueueReadLookupThenPut()
        val track = track(Hikka.DROPPED)
        tracker.bind(track, hasReadChapters = true).trackerId shouldBe 10L
        track.status shouldBe Hikka.READING
        track.remoteId shouldBe stringToNumber("test-manga-abc123")
        track.libraryId shouldBe null
        track.score shouldBe 8.0
        track.lastChapterRead shouldBe 12.0
        track.startedReadingDate shouldBe 1_690_000_000_000L
        track.finishedReadingDate shouldBe 0L

        harness.enqueue("read_no_content.json")
        harness.enqueue("manga.json")
        enqueueReadLookupThenPut()
        val kept = track(Hikka.DROPPED)
        tracker.bind(kept)
        kept.status shouldBe Hikka.ON_HOLD
        kept.finishedReadingDate shouldBe 1_695_000_000_000L
    }

    @Test
    fun bindWithoutReadStartsFresh() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.enqueue("manga_minimal.json")
        enqueueReadLookupThenPut()
        val read = track(Hikka.DROPPED)
        read.score = 4.0
        tracker.bind(read, hasReadChapters = true)
        read.status shouldBe Hikka.READING
        read.score shouldBe 0.0

        harness.enqueueRaw("", code = 404)
        harness.enqueue("manga_minimal.json")
        enqueueReadLookupThenPut()
        val planned = track(Hikka.DROPPED)
        tracker.bind(planned)
        planned.status shouldBe Hikka.PLAN_TO_READ
    }

    @Test
    fun bindKeepsACompletedRemote() = runTest {
        harness.enqueue("read_no_content.json")
        harness.enqueueRaw(fixture(MANGA).replace(""""status": "on_hold"""", """"status": "completed""""))
        enqueueReadLookupThenPut()
        val track = track(Hikka.READING)
        tracker.bind(track, hasReadChapters = true)
        track.status shouldBe Hikka.COMPLETED
    }

    /** No API status maps to REREADING, so only a stubbed API can hand one back. */
    @Test
    fun bindKeepsARereadingRemote() = runTest {
        mockkConstructor(HikkaApi::class)
        try {
            val remote = TrackSearch.create(10L).apply { status = Hikka.REREADING }
            val read = HKRead(
                reference = "r",
                note = null,
                updated = 0L,
                created = 0L,
                status = "reading",
                chapters = 2,
                volumes = 0,
                rereads = 1,
                score = 6,
            )
            coEvery { anyConstructed<HikkaApi>().getRead(any()) } returns read
            coEvery { anyConstructed<HikkaApi>().getManga(any()) } returns remote
            coEvery { anyConstructed<HikkaApi>().updateUserManga(any()) } answers { firstArg() }
            val track = track(Hikka.READING)
            tracker.bind(track, hasReadChapters = true)
            track.status shouldBe Hikka.REREADING
            track.lastChapterRead shouldBe 2.0
        } finally {
            unmockkAll()
        }
    }

    @Test
    fun refreshNeedsAReadEntry() = runTest {
        harness.enqueue("manga.json")
        harness.enqueue("read.json")
        val track = track(Hikka.DROPPED)
        tracker.refresh(track) shouldBe track
        track.totalChapters shouldBe 42L
        track.status shouldBe Hikka.READING
        track.score shouldBe 8.0
        track.lastChapterRead shouldBe 12.0
        track.startedReadingDate shouldBe 1_690_000_000_000L
        track.finishedReadingDate shouldBe 0L

        harness.enqueue("manga.json")
        harness.enqueue("read_no_content.json")
        tracker.refresh(track)
        track.startedReadingDate shouldBe 0L
        track.finishedReadingDate shouldBe 1_695_000_000_000L

        harness.enqueue("manga.json")
        harness.enqueueRaw("", code = 404)
        shouldThrow<NoSuchElementException> { tracker.refresh(track(Hikka.DROPPED)) }.message shouldBe
            "Could not find manga"
    }

    @Test
    fun searchAndDeleteGoThroughTheApi() = runTest {
        harness.enqueue("manga_search.json")
        tracker.search("first").size shouldBe 2
        harness.takeRequest().url.encodedPath shouldBe "/manga"

        harness.enqueueRaw("")
        tracker.delete(domainTrack(trackerId = 10L, remoteUrl = HikkaHarness.TRACKING_URL))
        harness.takeRequest().method shouldBe "DELETE"
    }

    private companion object {
        const val MANGA = "eu/kanade/tachiyomi/data/track/hikka/manga.json"
    }
}
