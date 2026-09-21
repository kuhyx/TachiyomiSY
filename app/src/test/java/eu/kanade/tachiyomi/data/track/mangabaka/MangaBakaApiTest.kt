package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.lang.toLocalDate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The library calls of [MangaBakaApi], served by a mock server behind the auth interceptor. */
internal class MangaBakaApiTest {

    private val harness = MangaBakaHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun fullTrack() = dbTrack(
        trackerId = 11L,
        remoteId = 1234L,
        status = MangaBaka.READING,
        lastChapterRead = 12.0,
        score = 150.0,
    ).also {
        it.private = true
        it.startedReadingDate = 1_700_000_000_000L
        it.finishedReadingDate = 1_710_000_000_000L
    }

    @Test
    fun addSendsOnlySetFields() = runTest {
        harness.enqueueRaw("""{"status":201,"data":true}""", code = 201)
        val track = fullTrack()
        harness.api.addLibManga(track) shouldBe track
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/v1/my/library/1234"
        request.headers["Authorization"] shouldBe "Bearer access-1"
        request.headers["User-Agent"] shouldStartWith "TachiyomiSY/v"
        val body = request.bodyText()
        body shouldContain """"is_private":true"""
        body shouldContain """"state":"reading""""
        body shouldContain """"progress_chapter":12.0"""
        body shouldContain """"rating":100"""
        body shouldContain """"start_date":"${1_700_000_000_000L.toLocalDate()}""""
        body shouldContain """"finish_date":"${1_710_000_000_000L.toLocalDate()}""""

        harness.enqueueRaw("""{"status":201,"data":true}""", code = 201)
        harness.api.addLibManga(dbTrack(trackerId = 11L, remoteId = 1L, status = MangaBaka.PLAN_TO_READ))
        val bare = harness.takeRequest().bodyText()
        bare shouldBe """{"is_private":false,"state":"plan_to_read"}"""
    }

    @Test
    fun updateSendsNullsForUnset() = runTest {
        harness.enqueueRaw("""{"status":200,"data":true}""")
        val track = fullTrack()
        harness.api.updateLibManga(track) shouldBe track
        val request = harness.takeRequest()
        request.method shouldBe "PUT"
        request.url.encodedPath shouldBe "/v1/my/library/1234"
        request.bodyText() shouldContain """"rating":100"""

        harness.enqueueRaw("""{"status":200,"data":true}""")
        harness.api.updateLibManga(dbTrack(trackerId = 11L, remoteId = 2L, status = MangaBaka.DROPPED))
        val bare = harness.takeRequest().bodyText()
        bare shouldBe """{"state":"dropped","is_private":false,"progress_chapter":null,"rating":null,""" +
            """"start_date":null,"finish_date":null}"""
    }

    @Test
    fun deleteHitsTheEntry() = runTest {
        harness.enqueueRaw("")
        harness.api.deleteLibManga(domainTrack(id = 1L, trackerId = 11L).copy(remoteId = 77L))
        val request = harness.takeRequest()
        request.method shouldBe "DELETE"
        request.url.encodedPath shouldBe "/v1/my/library/77"
    }

    @Test
    fun findMergesEntryAndSeries() = runTest {
        harness.enqueue("library_entry.json")
        harness.enqueue("series.json")
        val found = harness.api.findLibManga(dbTrack(trackerId = 11L, remoteId = 1234L))
        checkNotNull(found)
        found.trackerId shouldBe 11L
        found.remoteId shouldBe 1234L
        found.title shouldBe "Testing Story"
        found.status shouldBe MangaBaka.READING
        found.score shouldBe 80.0
        found.startedReadingDate shouldBe 1_704_164_645_000L
        found.finishedReadingDate shouldBe 1_706_933_106_000L
        found.lastChapterRead shouldBe 12.5
        found.private shouldBe true
        harness.takeRequest().url.encodedPath shouldBe "/v1/my/library/1234"
        harness.takeRequest().url.encodedPath shouldBe "/v1/series/1234"
    }

    @Test
    fun findDefaultsMissingFields() = runTest {
        harness.enqueue("library_entry_minimal.json")
        harness.enqueue("series.json")
        val found = harness.api.findLibManga(dbTrack(trackerId = 11L, remoteId = 1234L))
        checkNotNull(found)
        found.status shouldBe MangaBaka.PLAN_TO_READ
        found.score shouldBe 0.0
        found.startedReadingDate shouldBe 0L
        found.finishedReadingDate shouldBe 0L
        found.lastChapterRead shouldBe 0.0
        found.private shouldBe false
    }

    @Test
    fun findMissingIsNull() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.api.findLibManga(dbTrack(trackerId = 11L, remoteId = 5L)).shouldBeNull()
    }

    @Test
    fun findServerErrorThrows() = runTest {
        harness.enqueueRaw("", code = 500)
        val error = shouldThrow<HttpException> { harness.api.findLibManga(dbTrack(trackerId = 11L, remoteId = 5L)) }
        error.code shouldBe 500
        error.message shouldNotContain "404"
    }
}
