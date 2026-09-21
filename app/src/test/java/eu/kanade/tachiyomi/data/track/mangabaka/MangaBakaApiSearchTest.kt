package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaCover
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaItem
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaPublishData
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaScaledCover
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The search calls build Android [android.net.Uri]s, hence Robolectric. */
@RunWith(RobolectricTestRunner::class)
internal class MangaBakaApiSearchTest {

    private val harness = MangaBakaHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun searchParsesEveryItem() = runTest {
        harness.enqueue("search.json")
        val results = harness.api.search("test story")
        val request = harness.takeRequest()
        request.url.encodedPath shouldBe "/v1/series/search"
        request.url.queryParameter("q") shouldBe "test story"
        request.url.queryParameter("type_not") shouldBe "novel"
        request.headers["Authorization"] shouldBe null
        results.size shouldBe 2

        val full = results[0]
        full.trackerId shouldBe 11L
        full.remoteId shouldBe 1234L
        full.title shouldBe "Testing Story"
        full.summary shouldBe "A story about testing."
        full.score shouldBe 8.46
        full.coverUrl shouldBe "https://cdn.mangabaka.org/1234/x250.jpg"
        full.trackingUrl shouldBe "https://mangabaka.org/1234"
        full.startDate shouldBe "2019-04-01"
        full.publishingStatus shouldBe "releasing"
        full.publishingType shouldBe "Manga"
        full.authors shouldBe listOf("Author One")
        full.artists shouldBe listOf("Artist One")

        val bare = results[1]
        bare.remoteId shouldBe 99L
        bare.summary shouldBe ""
        bare.score shouldBe -1.0
        bare.coverUrl shouldBe ""
        bare.startDate shouldBe ""
        bare.publishingType shouldBe "Manhwa"
        bare.authors shouldBe emptyList()
        bare.artists shouldBe emptyList()
    }

    @Test
    fun emptyTypeStaysEmpty() {
        val item = MangaBakaItem(
            id = 1L,
            cover = MangaBakaCover(MangaBakaScaledCover("cover")),
            authors = emptyList(),
            artists = emptyList(),
            description = "d",
            published = MangaBakaPublishData("2020"),
            status = "s",
            type = "",
            rating = 7.0,
            titles = emptyList(),
        )
        val parsed = harness.api.parseSearchItem(item)
        parsed.publishingType shouldBe ""
        parsed.score shouldBe 7.0
        parsed.title shouldBe "ID: 1 - Could not find name! (report on the MangaBaka Discord)"
    }

    @Test
    fun detailsUseTheAuthClient() = runTest {
        harness.enqueue("series.json")
        val details = checkNotNull(harness.api.getMangaDetails(1234))
        details.title shouldBe "Testing Story"
        details.remoteId shouldBe 1234L
        val request = harness.takeRequest()
        request.url.encodedPath shouldBe "/v1/series/1234"
        request.headers["Authorization"] shouldBe "Bearer access-1"
    }

    @Test
    fun detailsMissingIsNull() = runTest {
        harness.enqueueRaw("", code = 404)
        harness.api.getMangaDetails(1).shouldBeNull()
    }

    @Test
    fun detailsOtherErrorsThrow() = runTest {
        harness.enqueueRaw("", code = 503)
        shouldThrow<HttpException> { harness.api.getMangaDetails(1) }.code shouldBe 503
    }
}
