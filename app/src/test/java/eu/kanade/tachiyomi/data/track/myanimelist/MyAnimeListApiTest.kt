package eu.kanade.tachiyomi.data.track.myanimelist

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MyAnimeListApiTest {

    private val server = FakeServer()
    private lateinit var api: MyAnimeListApi

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        api = MyAnimeListApi(TrackerManager.MYANIMELIST, server.client, mockk())
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(status: Long = MyAnimeList.READING) = dbTrack(
        trackerId = TrackerManager.MYANIMELIST,
        remoteId = 2L,
        status = status,
        lastChapterRead = 42.0,
        score = 8.0,
    )

    @Test
    fun accessTokenPostsPkceForm() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "oauth.json"))
        MyAnimeListApi.authUrl().toString() shouldContain "code_challenge="
        api.getAccessToken("code").accessToken shouldBe "acc"
        server.lastRequest.url.toString() shouldBe "https://myanimelist.net/v1/oauth2/token"
        server.bodies.single() shouldContain "code=code&code_verifier="
        server.bodies.single() shouldContain "grant_type=authorization_code"
    }

    @Test
    fun currentUserReadsName() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "user.json"))
        api.getCurrentUser() shouldBe "kuhy"
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/users/@me"
    }

    @Test
    fun searchDropsNovelsAndTrims() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "search.json"))
        val results = api.search("b".repeat(70))
        results.map { it.remoteId } shouldBe listOf(2L, 4L)
        val url = server.lastRequest.url
        url.queryParameter("q") shouldBe "b".repeat(64)
        url.queryParameter("nsfw") shouldBe "true"
        url.queryParameter("fields") shouldBe MyAnimeListApi.SEARCH_FIELDS
    }

    @Test
    fun mangaDetailsById() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "manga_details.json"))
        val result = api.getMangaDetails(2)
        result.title shouldBe "Berserk"
        result.authors shouldBe listOf("Kentarou Miura")
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/manga/2?fields=" +
            "id%2Ctitle%2Csynopsis%2Cnum_chapters%2Cmean%2Cmain_picture%2Cstatus%2Cmedia_type%2Cstart_date%2C" +
            "authors%7Bfirst_name%2Clast_name%7D"
    }

    @Test
    fun updateItemPutsFormAndParses() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "update_status.json"))
        val track = track(status = MyAnimeList.REREADING).apply {
            startedReadingDate = 1_700_000_000_000L
        }
        api.updateItem(track).status shouldBe MyAnimeList.PLAN_TO_READ
        server.lastRequest.method shouldBe "PUT"
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/manga/2/my_list_status"
        val body = server.bodies.single()
        body shouldContain "status=reading&is_rereading=true&score=8.0&num_chapters_read=42"
        body shouldContain "&start_date=20"
        body shouldContain "&finish_date="
    }

    @Test
    fun updateItemDefaultsStatus() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "update_status.json"))
        api.updateItem(track(status = 42L))
        server.bodies.single() shouldContain "status=reading&is_rereading=false"
    }

    @Test
    fun updateItemRejectsUnapproved() {
        server.enqueue(400, fixture("myanimelist", "invalid_content.json"))
        shouldThrow<MALTitleNotApproved> { runSuspend { api.updateItem(track()) } }
        server.enqueue(500, "{}")
        shouldThrow<HttpException> { runSuspend { api.updateItem(track()) } }.code shouldBe 500
    }

    @Test
    fun deleteItemCallsListStatus() = runSuspend {
        server.enqueue(200)
        api.deleteItem(domainTrack(TrackerManager.MYANIMELIST, remoteId = 2L))
        server.lastRequest.method shouldBe "DELETE"
        server.lastRequest.url.toString() shouldBe "https://api.myanimelist.net/v2/manga/2/my_list_status"
    }

    @Test
    fun refreshRequestCarriesOldToken() {
        val oauth = MALOAuth(tokenType = "Bearer", refreshToken = "ref", accessToken = "old", expiresIn = 1L)
        val request = MyAnimeListApi.refreshTokenRequest(oauth)
        request.header("Authorization") shouldBe "Bearer old"
        Buffer().also { checkNotNull(request.body).writeTo(it) }.readUtf8() shouldContain
            "refresh_token=ref&grant_type=refresh_token"
        MyAnimeListApi.mangaUrl(7L).toString() shouldBe "https://api.myanimelist.net/v2/manga/7/my_list_status"
    }
}
