package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.bodyText
import eu.kanade.tachiyomi.data.track.dbTrack
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The read paths of [HikkaApi]; they build Android [android.net.Uri]s, hence Robolectric. */
@RunWith(RobolectricTestRunner::class)
internal class HikkaApiTest {

    private val harness = HikkaHarness()
    private val track = dbTrack(trackerId = 10L, trackingUrl = HikkaHarness.TRACKING_URL)

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun searchPostsTheQuery() = runTest {
        harness.enqueue("manga_search.json")
        val results = harness.api.searchManga("first")
        results.map { it.title } shouldBe listOf("First EN", "Second")
        results[0].trackerId shouldBe 10L
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/manga"
        request.url.queryParameter("page") shouldBe "1"
        request.url.queryParameter("size") shouldBe "50"
        request.headers["auth"] shouldBe "hikka-secret"
        val body = request.bodyText()
        body shouldContain """"query":"first""""
        body shouldContain """"score":[0,10]"""
        body shouldContain """"sort":["score:desc","scored_by:desc"]"""
        body shouldContain """"only_translated":false"""
    }

    @Test
    fun getReadParsesTheEntry() = runTest {
        harness.enqueue("read.json")
        val read = checkNotNull(harness.api.getRead(track))
        read.chapters shouldBe 12
        read.status shouldBe "reading"
        val request = harness.takeRequest()
        request.method shouldBe "GET"
        request.url.encodedPath shouldBe "/read/manga/test-manga-abc123"
    }

    @Test
    fun getReadMissingIsNull() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.api.getRead(track).shouldBeNull()
    }

    @Test
    fun getReadOtherErrorsThrow() = runTest {
        harness.enqueueRaw("", code = 500)
        shouldThrow<HttpException> { harness.api.getRead(track) }.code shouldBe 500
    }

    @Test
    fun getMangaBecomesATrack() = runTest {
        harness.enqueue("manga.json")
        val remote = harness.api.getManga(track)
        remote.title shouldBe "Назва"
        remote.totalChapters shouldBe 42L
        remote.trackingUrl shouldBe HikkaHarness.TRACKING_URL
        harness.takeRequest().url.encodedPath shouldBe "/manga/test-manga-abc123"
    }

    @Test
    fun deleteUsesTheRemoteUrl() = runTest {
        harness.enqueueRaw("")
        harness.api.deleteUserManga(domainTrack(trackerId = 10L, remoteUrl = HikkaHarness.TRACKING_URL))
        val request = harness.takeRequest()
        request.method shouldBe "DELETE"
        request.url.encodedPath shouldBe "/read/manga/test-manga-abc123"
    }
}
