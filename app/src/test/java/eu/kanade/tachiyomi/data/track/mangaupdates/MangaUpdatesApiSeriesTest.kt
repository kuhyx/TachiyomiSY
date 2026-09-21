package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MangaUpdatesApiSeriesTest {

    private val server = FakeServer()
    private lateinit var api: MangaUpdatesApi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        api = MangaUpdatesApi(mockk(), server.client)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun searchPostsFilters() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "search.json"))
        api.search("berserk").map { it.seriesId } shouldBe listOf(87_654_321L, null, null)
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/series/search"
        server.bodies.single() shouldBe """{"search":"berserk","filter_types":["drama cd","novel"]}"""
    }

    @Test
    fun authenticatePutsCredentials() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "login.json"))
        val context = api.authenticate("kuhy", "pass")
        context.sessionToken shouldBe "sess"
        context.uid shouldBe 555L
        server.lastRequest.method shouldBe "PUT"
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/account/login"
        server.bodies.single() shouldBe """{"username":"kuhy","password":"pass"}"""
    }

    @Test
    fun currentUserReadsProfile() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "profile.json"))
        api.getCurrentUser().username shouldBe "kuhy"
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/account/profile"
    }

    @Test
    fun seriesByTrackOrId() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "series.json"))
        val series = api.getSeries(domainTrack(TrackerManager.MANGAUPDATES, remoteId = 87_654_321L))
        series.title shouldBe "Berserk &amp; Co"
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/series/87654321"
        server.enqueue(200, fixture("mangaupdates", "series_bare.json"))
        api.getSeries(2L).title shouldBe "Bare"
    }

    @Test
    fun legacyIdFollowsRedirect() = runSuspend {
        server.enqueue(308, headers = headersOf("Location", "https://www.mangaupdates.com/series/abc123/berserk"))
        api.convertToNewId(15) shouldBe "abc123"
        server.lastRequest.url.toString() shouldBe "https://www.mangaupdates.com/series.html?id=15"
        server.enqueue(308, headers = headersOf("Location", "https://www.mangaupdates.com/series/xyz/"))
        api.convertToNewId(15) shouldBe "xyz"
        server.enqueue(308, headers = headersOf("Location", "https://www.mangaupdates.com/series/q9"))
        api.convertToNewId(15) shouldBe "q9"
    }

    @Test
    fun legacyIdWithoutRedirect() = runSuspend {
        server.enqueue(200)
        api.convertToNewId(15).shouldBeNull()
        server.enqueue(308)
        api.convertToNewId(15).shouldBeNull()
        server.enqueue(308, headers = headersOf("Location", "https://www.mangaupdates.com/"))
        api.convertToNewId(15).shouldBeNull()
    }
}
