package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [HikkaApi.addUserManga] / [HikkaApi.updateUserManga]: the PUT payload and the reread counter. */
@RunWith(RobolectricTestRunner::class)
internal class HikkaApiWriteTest {

    private val harness = HikkaHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    private fun track(status: Long, lastChapterRead: Double = 3.0): Track = dbTrack(
        trackerId = 10L,
        status = status,
        lastChapterRead = lastChapterRead,
        score = 7.0,
        trackingUrl = HikkaHarness.TRACKING_URL,
    )

    private fun putBody(): String {
        harness.takeRequest()
        val put = harness.takeRequest()
        put.method shouldBe "PUT"
        put.url.encodedPath shouldBe "/read/manga/test-manga-abc123"
        return put.bodyText()
    }

    @Test
    fun newEntryStartsWithoutRereads() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.enqueue("read.json")
        val track = track(Hikka.READING).also {
            it.startedReadingDate = 1_690_000_000_000L
            it.finishedReadingDate = 0L
        }
        val result = harness.api.addUserManga(track)
        result.lastChapterRead shouldBe 12.0
        result.trackerId shouldBe 10L
        val body = putBody()
        body shouldContain """"rereads":0"""
        body shouldContain """"chapters":3"""
        body shouldContain """"score":7"""
        body shouldContain """"status":"reading""""
        body shouldContain """"start_date":1690000000"""
        body shouldContain """"end_date":null"""
    }

    @Test
    fun rereadingBumpsTheCounter() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.enqueue("read.json")
        harness.api.updateUserManga(track(Hikka.REREADING))
        putBody() shouldContain """"rereads":1"""
    }

    @Test
    fun existingRereadsAreKept() = runTest {
        harness.enqueue("read_no_content.json")
        harness.enqueue("read.json")
        harness.api.addUserManga(track(Hikka.REREADING))
        putBody() shouldContain """"rereads":3"""

        harness.enqueue("read_no_content.json")
        harness.enqueue("read.json")
        harness.api.addUserManga(track(Hikka.COMPLETED))
        putBody() shouldContain """"rereads":3"""
    }

    @Test
    fun secondsConversion() {
        0L.toApiSeconds() shouldBe null
        (-5L).toApiSeconds() shouldBe null
        1_690_000_000_999L.toApiSeconds() shouldBe 1_690_000_000L
    }
}
