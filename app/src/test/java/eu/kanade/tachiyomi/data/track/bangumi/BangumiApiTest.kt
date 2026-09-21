package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BangumiApiTest {

    private val server = FakeServer()
    private lateinit var api: BangumiApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = BangumiApi(TrackerManager.BANGUMI, server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(score: Double = 8.0) = dbTrack(
        trackerId = TrackerManager.BANGUMI,
        remoteId = 1L,
        status = Bangumi.READING,
        lastChapterRead = 12.0,
        score = score,
    ).apply { private = true }

    @Test
    fun addLibMangaPostsCollection() = runSuspend {
        server.enqueue(202)
        api.addLibManga(track(score = 15.0)).remoteId shouldBe 1L
        server.lastRequest.method shouldBe "POST"
        server.lastRequest.url.toString() shouldBe "https://api.bgm.tv/v0/users/-/collections/1"
        server.lastRequest.header("Content-Type") shouldBe "application/json"
        server.bodies.single() shouldBe """{"type":3,"rate":10,"ep_status":12,"private":true}"""
    }

    @Test
    fun updateLibMangaPatches() = runSuspend {
        server.enqueue(204)
        api.updateLibManga(track(score = -3.0)).remoteId shouldBe 1L
        server.lastRequest.method shouldBe "PATCH"
        server.bodies.single() shouldBe """{"type":3,"rate":0,"ep_status":12,"private":true}"""
    }

    @Test
    fun searchKeepsMangaPlatforms() = runSuspend {
        server.enqueue(200, fixture("bangumi", "search.json"))
        api.search("slam").map { it.remoteId } shouldBe listOf(1L, 2L)
        server.lastRequest.url.toString() shouldBe "https://api.bgm.tv/v0/search/subjects?limit=20"
        server.bodies.single() shouldBe """{"keyword":"slam","sort":"match","filter":{"type":[1]}}"""
    }

    @Test
    fun statusLibMangaFillsTrack() = runSuspend {
        server.enqueue(200, fixture("bangumi", "collection.json"))
        val track = checkNotNull(api.statusLibManga(track(), "kuhy42"))
        track.status shouldBe Bangumi.READING
        track.lastChapterRead shouldBe 12.0
        track.score shouldBe 8.0
        track.totalChapters shouldBe 276L
        server.lastRequest.url.toString() shouldBe "https://api.bgm.tv/v0/users/kuhy42/collections/1"
        server.lastRequest.header("Cache-Control") shouldBe "no-cache"
    }

    @Test
    fun statusLibMangaBareCollection() = runSuspend {
        server.enqueue(200, fixture("bangumi", "collection_bare.json"))
        val track = checkNotNull(api.statusLibManga(track(), "kuhy42"))
        track.status shouldBe Bangumi.PLAN_TO_READ
        track.lastChapterRead shouldBe 0.0
        track.score shouldBe 0.0
        track.totalChapters shouldBe 0L
    }

    @Test
    fun statusLibMangaNotCollected() = runSuspend {
        server.enqueue(404)
        api.statusLibManga(track(), "kuhy42").shouldBeNull()
        server.enqueue(500)
        shouldThrow<HttpException> { api.statusLibManga(track(), "kuhy42") }.code shouldBe 500
    }

    @Test
    fun metadataReadsInfobox() = runSuspend {
        server.enqueue(200, fixture("bangumi", "subject.json"))
        api.getMangaMetadata(domainTrack(TrackerManager.BANGUMI, remoteId = 1L)) shouldBe TrackMangaMetadata(
            remoteId = 1L,
            title = "灌篮高手",
            thumbnailUrl = "https://img/1.jpg",
            description = "Basketball.",
            authors = "井上雄彦",
            artists = "Painter",
        )
        server.lastRequest.url.toString() shouldBe "https://api.bgm.tv/v0/subjects/1"
    }

    @Test
    fun metadataWithoutImages() = runSuspend {
        server.enqueue(200, fixture("bangumi", "subject_bare.json"))
        val bare = api.getMangaMetadata(domainTrack(TrackerManager.BANGUMI, remoteId = 2L))
        bare.thumbnailUrl.shouldBeNull()
        bare.authors shouldBe ""
        bare.artists shouldBe ""
    }

    @Test
    fun accessTokenAndUser() = runSuspend {
        server.enqueue(200, fixture("bangumi", "oauth.json"))
        api.accessToken("code").accessToken shouldBe "acc"
        server.lastRequest.url.toString() shouldBe "https://bgm.tv/oauth/access_token"
        server.bodies.single() shouldContain "grant_type=authorization_code"
        server.bodies.single() shouldContain "&code=code&redirect_uri=mihon%3A%2F%2Fbangumi-auth"
        server.enqueue(200, fixture("bangumi", "user.json"))
        api.getCurrentUser().username shouldBe "kuhy42"
        server.lastRequest.url.toString() shouldBe "https://api.bgm.tv/v0/me"
    }

    @Test
    fun authUrlCarriesRedirect() {
        BangumiApi.authUrl().toString() shouldBe "https://bgm.tv/oauth/authorize?client_id=bgm291665acbd06a4c28&" +
            "response_type=code&redirect_uri=mihon%3A%2F%2Fbangumi-auth"
    }
}
