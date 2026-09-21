package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.bodyText
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The OAuth calls of [MangaBakaApi]: the token exchange, the profile and the refresh request. */
internal class MangaBakaApiAuthTest {

    private val harness = MangaBakaHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun accessTokenPostsTheCode() = runTest {
        MangaBakaApi.codeVerifier = "verifier-1"
        harness.enqueue("oauth.json")
        val oauth = harness.api.getAccessToken("code-1")
        oauth.accessToken shouldBe "access-1"
        oauth.refreshToken shouldBe "refresh-1"
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.url.encodedPath shouldBe "/auth/oauth2/token"
        request.headers["Authorization"] shouldBe null
        val body = request.bodyText()
        body shouldContain "client_id=${MangaBakaApi.CLIENT_ID}"
        body shouldContain "code=code-1"
        body shouldContain "code_verifier=verifier-1"
        body shouldContain "grant_type=authorization_code"
        body shouldContain "redirect_uri=mihon%3A%2F%2Fmangabaka-auth"
    }

    @Test
    fun currentUserIsAuthenticated() = runTest {
        harness.enqueue("profile.json")
        val profile = harness.api.getCurrentUser()
        profile.id shouldBe "user-id-1"
        profile.ratingSteps shouldBe 5
        val request = harness.takeRequest()
        request.method shouldBe "GET"
        request.url.encodedPath shouldBe "/v1/my/profile"
        request.headers["Authorization"] shouldBe "Bearer access-1"
    }

    @Test
    fun refreshRequestIsAForm() {
        val request = MangaBakaApi.refreshTokenRequest("refresh-9")
        request.method shouldBe "POST"
        request.url.toString() shouldBe "https://mangabaka.org/auth/oauth2/token"
        val body = Buffer().also { checkNotNull(request.body).writeTo(it) }.readUtf8()
        body shouldContain "grant_type=refresh_token"
        body shouldContain "refresh_token=refresh-9"
        body shouldContain "client_id=${MangaBakaApi.CLIENT_ID}"
    }

    @Test
    fun oauthStateStartsEmpty() {
        harness.api.verifyOAuthState("anything") shouldBe false
        MangaBakaApi.libraryEntryUrl(3L) shouldBe "https://api.mangabaka.org/v1/my/library/3"
        MangaBakaApi.SCOPES shouldBe "library.read library.write offline_access openid"
    }
}
