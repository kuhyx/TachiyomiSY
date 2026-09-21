package eu.kanade.tachiyomi.data.track.hikka

import eu.kanade.tachiyomi.data.track.bodyText
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The auth endpoints and request builders of [HikkaApi]. */
@RunWith(RobolectricTestRunner::class)
internal class HikkaApiAuthTest {

    private val harness = HikkaHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun currentUserIsAuthenticated() = runTest {
        harness.enqueue("user.json")
        val user = harness.api.getCurrentUser()
        user.reference shouldBe "user-ref"
        user.username shouldBe "hikka_user"
        val request = harness.takeRequest()
        request.method shouldBe "GET"
        request.url.encodedPath shouldBe "/user/me"
        request.headers["auth"] shouldBe "hikka-secret"
    }

    @Test
    fun accessTokenPostsTheReference() = runTest {
        harness.enqueue("oauth.json")
        val oauth = harness.api.accessToken("ref-1")
        oauth.accessToken shouldBe "hikka-secret"
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/auth/token"
        request.headers["auth"] shouldBe null
        val body = request.bodyText()
        body shouldContain """"request_reference":"ref-1""""
        body shouldContain """"client_secret":"""
    }

    @Test
    fun authUrlNamesTheClient() {
        val url = HikkaApi.authUrl()
        url.scheme shouldBe "https"
        url.host shouldBe "hikka.io"
        url.path shouldBe "/oauth"
        url.getQueryParameter("reference") shouldBe "598ef1f5-b9d2-4e66-8b65-06949d5e14fc"
        url.getQueryParameter("scope") shouldBe "readlist,read:user-details"
    }

    @Test
    fun requestBuilders() {
        val refresh = HikkaApi.refreshTokenRequest("tok")
        refresh.method shouldBe "GET"
        refresh.url.toString() shouldBe "https://api.hikka.io/user/me"
        refresh.header("auth") shouldBe "tok"

        val info = HikkaApi.authTokenInfo("tok")
        info.url.toString() shouldBe "https://api.hikka.io/auth/token/info"
        info.header("auth") shouldBe "tok"

        val create = HikkaApi.authTokenCreate("ref")
        create.url.toString() shouldBe "https://api.hikka.io/auth/token"
        Buffer().also { checkNotNull(create.body).writeTo(it) }.readUtf8() shouldContain """"request_reference":"ref""""

        HikkaApi.readMangaUrl("slug") shouldBe "https://api.hikka.io/read/manga/slug"
    }
}
