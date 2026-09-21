package eu.kanade.tachiyomi.data.track.mangabaka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveMinLength
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [MangaBakaApi.authUrl] builds an Android [android.net.Uri], so this one runs under Robolectric. */
@RunWith(RobolectricTestRunner::class)
internal class MangaBakaApiUrlTest {

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
    fun authUrlCarriesPkceAndState() {
        val url = MangaBakaApi.authUrl()
        url.scheme shouldBe "https"
        url.host shouldBe "mangabaka.org"
        url.path shouldBe "/auth/oauth2/authorize"
        url.getQueryParameter("client_id") shouldBe MangaBakaApi.CLIENT_ID
        url.getQueryParameter("code_challenge_method") shouldBe "S256"
        url.getQueryParameter("response_type") shouldBe "code"
        url.getQueryParameter("scope") shouldBe MangaBakaApi.SCOPES
        url.getQueryParameter("redirect_uri") shouldBe MangaBakaApi.REDIRECT_URI
        checkNotNull(url.getQueryParameter("code_challenge")) shouldHaveMinLength 40
        MangaBakaApi.codeVerifier shouldHaveMinLength 40

        val state = checkNotNull(url.getQueryParameter("state"))
        harness.api.verifyOAuthState(state) shouldBe true
        harness.api.verifyOAuthState(state + "x") shouldBe false
        harness.tracker.verifyOAuthState(state) shouldBe true

        MangaBakaApi.authUrl().getQueryParameter("state") shouldNotBe state
        harness.api.verifyOAuthState(state) shouldBe false
    }
}
