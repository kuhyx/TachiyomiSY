package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** [MangaBaka.update] and [MangaBaka.bind]: the status transitions and what goes over the wire. */
internal class MangaBakaSyncTest {

    private val harness = MangaBakaHarness()
    private val tracker: MangaBaka
        get() = harness.tracker

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun track(status: Long, lastChapterRead: Double = 0.0, totalChapters: Long = 0L): Track = dbTrack(
        trackerId = 11L,
        remoteId = 1234L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    private fun enqueueOk() = harness.enqueueRaw("""{"status":200,"data":true}""")

    @Test
    fun updateWithoutReadKeepsStatus() = runTest {
        enqueueOk()
        val plain = track(MangaBaka.PAUSED, lastChapterRead = 3.0, totalChapters = 3L)
        tracker.update(plain) shouldBe plain
        plain.status shouldBe MangaBaka.PAUSED
        harness.takeRequest().method shouldBe "PUT"

        enqueueOk()
        val done = track(MangaBaka.COMPLETED, lastChapterRead = 3.0, totalChapters = 3L)
        tracker.update(done, didReadChapter = true)
        done.status shouldBe MangaBaka.COMPLETED
        done.finishedReadingDate shouldBe 0L
    }

    @Test
    fun readingTheLastChapterCompletes() = runTest {
        enqueueOk()
        val before = System.currentTimeMillis()
        val last = track(MangaBaka.READING, lastChapterRead = 10.0, totalChapters = 10L)
        tracker.update(last, didReadChapter = true)
        last.status shouldBe MangaBaka.COMPLETED
        last.finishedReadingDate shouldBeGreaterThan before - 1
        harness.takeRequest().bodyText() shouldContain """"state":"completed""""
    }

    @Test
    fun readingStartsOrKeepsRereading() = runTest {
        enqueueOk()
        val first = track(MangaBaka.PLAN_TO_READ, lastChapterRead = 1.0, totalChapters = 10L)
        tracker.update(first, didReadChapter = true)
        first.status shouldBe MangaBaka.READING
        first.startedReadingDate shouldBeGreaterThan 0L

        enqueueOk()
        val later = track(MangaBaka.PAUSED, lastChapterRead = 5.0)
        tracker.update(later, didReadChapter = true)
        later.status shouldBe MangaBaka.READING
        later.startedReadingDate shouldBe 0L

        enqueueOk()
        val again = track(MangaBaka.REREADING, lastChapterRead = 2.0, totalChapters = 10L)
        tracker.update(again, didReadChapter = true)
        again.status shouldBe MangaBaka.REREADING
    }

    @Test
    fun bindFoundStartsReading() = runTest {
        harness.enqueueEntry("plan_to_read")
        harness.enqueue("series.json")
        enqueueOk()
        val track = track(MangaBaka.PAUSED)
        track.private = true
        tracker.bind(track, hasReadChapters = true) shouldBe track
        track.status shouldBe MangaBaka.READING
        track.title shouldBe "Testing Story"
        track.remoteId shouldBe 1234L
        track.private shouldBe true
        repeat(2) { harness.takeRequest() }
        harness.takeRequest().method shouldBe "PUT"
    }

    @Test
    fun bindFoundKeepsRemoteStatus() = runTest {
        harness.enqueueEntry("rereading")
        harness.enqueue("series.json")
        enqueueOk()
        val rereading = track(MangaBaka.PAUSED)
        tracker.bind(rereading, hasReadChapters = true)
        rereading.status shouldBe MangaBaka.REREADING

        harness.enqueueEntry("paused")
        harness.enqueue("series.json")
        enqueueOk()
        val unread = track(MangaBaka.READING)
        tracker.bind(unread)
        unread.status shouldBe MangaBaka.PAUSED

        harness.enqueueEntry("completed")
        harness.enqueue("series.json")
        enqueueOk()
        val done = track(MangaBaka.READING)
        tracker.bind(done, hasReadChapters = true)
        done.status shouldBe MangaBaka.COMPLETED
    }

    @Test
    fun bindMissingAddsAnEntry() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.enqueueRaw("""{"status":201,"data":true}""", code = 201)
        val read = track(MangaBaka.PAUSED, lastChapterRead = 4.0)
        read.score = 50.0
        tracker.bind(read, hasReadChapters = true) shouldBe read
        read.status shouldBe MangaBaka.READING
        read.score shouldBe 0.0
        harness.takeRequest()
        harness.takeRequest().method shouldBe "POST"

        harness.enqueueRaw("", code = 404)
        harness.enqueueRaw("""{"status":201,"data":true}""", code = 201)
        val fresh = track(MangaBaka.PAUSED)
        tracker.bind(fresh)
        fresh.status shouldBe MangaBaka.PLAN_TO_READ
    }
}
