package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnilistApiTest {

    private val server = FakeServer()
    private lateinit var api: AnilistApi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        api = AnilistApi(server.client, mockk())
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track() = dbTrack(
        trackerId = TrackerManager.ANILIST,
        remoteId = 101L,
        libraryId = 5001L,
        status = Anilist.READING,
        lastChapterRead = 12.0,
        score = 80.0,
    ).apply {
        startedReadingDate = 1_700_000_000_000L
        private = true
    }

    @Test
    fun addLibMangaStoresEntryId() = runSuspend {
        server.enqueue(200, fixture("anilist", "add_manga.json"))
        val track = track().apply { libraryId = null }
        api.addLibManga(track).libraryId shouldBe 5001L
        server.lastRequest.url.toString() shouldBe AnilistApi.API_URL
        server.lastRequest.method shouldBe "POST"
        val body = server.bodies.single()
        body shouldContain "mutation AddManga"
        body shouldContain """"mangaId":101"""
        body shouldContain """"progress":12"""
        body shouldContain """"status":"CURRENT""""
        body shouldContain """"private":true"""
    }

    @Test
    fun updateLibMangaSendsDates() = runSuspend {
        server.enqueue(200)
        api.updateLibManga(track()).libraryId shouldBe 5001L
        val body = server.bodies.single()
        body shouldContain "mutation UpdateManga"
        body shouldContain """"listId":5001"""
        body shouldContain """"score":80"""
        body shouldContain """"startedAt":{"year":"""
        body shouldContain """"completedAt":{"year":null,"month":null,"day":null}"""
    }

    @Test
    fun updateLibMangaHttpError() {
        server.enqueue(500)
        shouldThrow<HttpException> { runSuspend { api.updateLibManga(track()) } }.code shouldBe 500
    }

    @Test
    fun deleteLibMangaSendsListId() = runSuspend {
        server.enqueue(200)
        api.deleteLibManga(domainTrack(TrackerManager.ANILIST, libraryId = 5001L))
        server.bodies.single() shouldContain "mutation DeleteManga"
        server.bodies.single() shouldContain """"listId":5001"""
    }

    @Test
    fun findLibMangaMapsFirstEntry() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list.json"))
        val found = checkNotNull(api.findLibManga(track(), 777))
        found.libraryId shouldBe 5001L
        found.remoteId shouldBe 101L
        found.status shouldBe Anilist.READING
        found.score shouldBe 80.0
        found.lastChapterRead shouldBe 12.0
        found.private shouldBe true
        found.totalChapters shouldBe 179L
        found.finishedReadingDate shouldBe 0L
        (found.startedReadingDate > 0L) shouldBe true
        server.bodies.single() shouldContain """"id":777"""
        server.bodies.single() shouldContain """"manga_id":101"""
    }

    @Test
    fun findLibMangaIsNullWhenAbsent() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        api.findLibManga(track(), 777).shouldBeNull()
    }

    @Test
    fun getLibMangaFailsWhenAbsent() {
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        shouldThrow<NoSuchElementException> { runSuspend { api.getLibManga(track(), 777) } }
        server.enqueue(200, fixture("anilist", "user_list.json"))
        runSuspend { api.getLibManga(track(), 777).title shouldBe "Solo Leveling" }
    }

    @Test
    fun createOAuthLastsAYear() {
        val before = System.currentTimeMillis()
        val oauth = api.createOAuth("tok")
        oauth.accessToken shouldBe "tok"
        oauth.tokenType shouldBe "Bearer"
        oauth.expiresIn shouldBe 365L * 24L * 60L * 60L * 1000L
        (oauth.expires >= before + oauth.expiresIn) shouldBe true
        AnilistApi.mangaUrl(101L) shouldBe "https://anilist.co/manga/101"
    }
}
