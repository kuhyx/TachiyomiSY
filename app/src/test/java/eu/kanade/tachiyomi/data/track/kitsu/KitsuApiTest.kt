package eu.kanade.tachiyomi.data.track.kitsu

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
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KitsuApiTest {

    private val server = FakeServer()
    private lateinit var api: KitsuApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = KitsuApi(server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(libraryId: Long? = 501L) = dbTrack(
        trackerId = TrackerManager.KITSU,
        remoteId = 42L,
        libraryId = libraryId,
        status = Kitsu.READING,
        lastChapterRead = 12.0,
        score = 7.5,
    ).apply {
        startedReadingDate = 1_700_000_000_000L
        private = true
    }

    @Test
    fun addLibMangaPostsEntry() = runSuspend {
        server.enqueue(200, fixture("kitsu", "add_manga.json"))
        val result = api.addLibManga(track(libraryId = null), "9001")
        result.libraryId shouldBe 77L
        val request = server.lastRequest
        request.method shouldBe "POST"
        request.url.toString() shouldBe "https://kitsu.app/api/edge/library-entries"
        request.header("Content-Type") shouldBe "application/vnd.api+json"
        server.bodies.single() shouldContain """"status":"current""""
        server.bodies.single() shouldContain """"id":"9001""""
        server.bodies.single() shouldContain """"id":42"""
    }

    @Test
    fun updateLibMangaPatchesEntry() = runSuspend {
        server.enqueue(200)
        api.updateLibManga(track()).libraryId shouldBe 501L
        val request = server.lastRequest
        request.method shouldBe "PATCH"
        request.url.toString() shouldBe "https://kitsu.app/api/edge/library-entries/501"
        val body = server.bodies.single()
        body shouldContain """"ratingTwenty":"15""""
        body shouldContain """"progress":12"""
        body shouldContain """"private":true"""
        body shouldContain """"startedAt":""""
        body shouldContain """"finishedAt":null"""
    }

    @Test
    fun updateLibMangaHttpError() {
        server.enqueue(500)
        shouldThrow<HttpException> { runBlocking { api.updateLibManga(track()) } }.code shouldBe 500
    }

    @Test
    fun removeLibMangaDeletesEntry() = runSuspend {
        server.enqueue(204)
        api.removeLibManga(domainTrack(TrackerManager.KITSU, libraryId = 501L))
        server.lastRequest.method shouldBe "DELETE"
        server.lastRequest.url.toString() shouldBe "https://kitsu.app/api/edge/library-entries/501"
    }

    @Test
    fun findLibMangaReturnsMatch() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries.json"))
        val found = checkNotNull(api.findLibManga(track(), "9001"))
        found.libraryId shouldBe 501L
        found.remoteId shouldBe 42L
        server.lastRequest.url.toString() shouldBe
            "https://kitsu.app/api/edge/library-entries?filter[manga_id]=42&filter[user_id]=9001&include=manga"
    }

    @Test
    fun findLibMangaIsNullWithoutEntry() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries_empty.json"))
        api.findLibManga(track(), "9001").shouldBeNull()
        server.enqueue(200, fixture("kitsu", "library_entries_no_included.json"))
        api.findLibManga(track(), "9001").shouldBeNull()
    }

    @Test
    fun getLibMangaReturnsMatch() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries.json"))
        api.getLibManga(track()).title shouldBe "One Piece"
        server.lastRequest.url.toString() shouldBe
            "https://kitsu.app/api/edge/library-entries?filter[id]=501&include=manga"
    }

    @Test
    fun getLibMangaFailsWithoutEntry() {
        server.enqueue(200, fixture("kitsu", "library_entries_empty.json"))
        shouldThrow<NoSuchElementException> { runBlocking { api.getLibManga(track()) } }
        server.enqueue(200, fixture("kitsu", "library_entries_no_included.json"))
        shouldThrow<NoSuchElementException> { runBlocking { api.getLibManga(track()) } }
    }

    @Test
    fun loginPostsCredentials() = runSuspend {
        server.enqueue(200, fixture("kitsu", "oauth.json"))
        api.login("user", "pass").accessToken shouldBe "acc"
        val request = server.lastRequest
        request.url.toString() shouldBe "https://kitsu.app/api/oauth/token"
        server.bodies.single() shouldContain "username=user&password=pass&grant_type=password"
    }

    @Test
    fun getCurrentUserReadsFirstUser() = runSuspend {
        server.enqueue(200, fixture("kitsu", "users.json"))
        val user = api.getCurrentUser()
        user.id shouldBe "9001"
        user.attributes.name shouldBe "kuhy"
        server.lastRequest.url.toString() shouldBe "https://kitsu.app/api/edge/users?filter[self]=true"
        KitsuApi.mangaUrl(5L) shouldBe "https://kitsu.app/manga/5"
        val refresh = KitsuApi.refreshTokenRequest("tok")
        Buffer().also { checkNotNull(refresh.body).writeTo(it) }.readUtf8() shouldContain "refresh_token=tok"
    }
}
