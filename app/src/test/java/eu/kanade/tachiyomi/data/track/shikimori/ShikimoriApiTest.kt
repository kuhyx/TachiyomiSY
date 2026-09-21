package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ShikimoriApiTest {

    private val server = FakeServer()
    private lateinit var api: ShikimoriApi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        api = ShikimoriApi(TrackerManager.SHIKIMORI, server.client, mockk())
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track() = dbTrack(
        trackerId = TrackerManager.SHIKIMORI,
        remoteId = 2L,
        status = Shikimori.READING,
        lastChapterRead = 42.0,
        score = 8.0,
    )

    @Test
    fun addLibMangaPostsRate() = runSuspend {
        server.enqueue(200, fixture("shikimori", "add_manga.json"))
        api.addLibManga(track(), "31337").libraryId shouldBe 9001L
        server.lastRequest.url.toString() shouldBe "https://shikimori.io/api/v2/user_rates"
        server.lastRequest.method shouldBe "POST"
        val body = server.bodies.single()
        body shouldContain """"user_id":"31337""""
        body shouldContain """"target_id":2"""
        body shouldContain """"target_type":"Manga""""
        body shouldContain """"chapters":42"""
        body shouldContain """"score":8"""
        body shouldContain """"status":"watching""""
    }

    @Test
    fun updateLibMangaIsAnAdd() = runSuspend {
        server.enqueue(200, fixture("shikimori", "add_manga.json"))
        api.updateLibManga(track(), "31337").libraryId shouldBe 9001L
        server.enqueue(500)
        shouldThrow<HttpException> { api.updateLibManga(track(), "31337") }
    }

    @Test
    fun deleteLibMangaTargetsRate() = runSuspend {
        server.enqueue(200)
        api.deleteLibManga(domainTrack(TrackerManager.SHIKIMORI, libraryId = 9001L))
        server.lastRequest.method shouldBe "DELETE"
        server.lastRequest.url.toString() shouldBe "https://shikimori.io/api/v2/user_rates/9001"
    }

    @Test
    fun findLibMangaReturnsEntry() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list.json"))
        val found = checkNotNull(api.findLibManga(track()))
        found.libraryId shouldBe 9001L
        found.lastChapterRead shouldBe 42.0
        server.lastRequest.url.toString() shouldBe ShikimoriApi.GRAPHQL_API_URL
        server.bodies.single() shouldContain """"id":"2""""
    }

    @Test
    fun findLibMangaWithoutRate() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list_no_rate.json"))
        checkNotNull(api.findLibManga(track())).libraryId.shouldBeNull()
        server.enqueue(200, fixture("shikimori", "user_list_no_rate.json"))
        api.findLibManga(track(), isRefresh = true).shouldBeNull()
        server.enqueue(200, fixture("shikimori", "user_list.json"))
        checkNotNull(api.findLibManga(track(), isRefresh = true)).libraryId shouldBe 9001L
    }

    @Test
    fun findLibMangaUnknownTitle() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list_empty.json"))
        api.findLibManga(track()).shouldBeNull()
        server.enqueue(200, fixture("shikimori", "user_list_empty.json"))
        api.findLibManga(track(), isRefresh = true).shouldBeNull()
    }

    @Test
    fun currentUserViaGraphql() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user.json"))
        val user = api.getCurrentUser()
        user.id shouldBe "31337"
        user.nickname shouldBe "kuhy"
        server.bodies.single() shouldContain "currentUser"
    }

    @Test
    fun accessTokenPostsCode() = runSuspend {
        server.enqueue(200, fixture("shikimori", "oauth.json"))
        api.accessToken("code").accessToken shouldBe "acc"
        server.lastRequest.url.toString() shouldBe "https://shikimori.io/oauth/token"
        server.bodies.single() shouldContain "grant_type=authorization_code"
        server.bodies.single() shouldContain "&code=code&redirect_uri=mihon%3A%2F%2Fshikimori-auth"
    }
}
