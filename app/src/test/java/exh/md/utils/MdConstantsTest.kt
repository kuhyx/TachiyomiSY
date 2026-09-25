package exh.md.utils

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MdConstantsTest {
    @Test
    fun constants() {
        MdConstants.baseUrl shouldBe "https://mangadex.org"
        MdConstants.cdnUrl shouldBe "https://uploads.mangadex.org"
        MdConstants.atHomeReportUrl shouldBe "https://api.mangadex.network/report"
        MdConstants.Types.author shouldBe "author"
        MdConstants.Types.artist shouldBe "artist"
        MdConstants.Types.coverArt shouldBe "cover_art"
        MdConstants.Types.manga shouldBe "manga"
        MdConstants.Types.scanlator shouldBe "scanlation_group"
        MdConstants.mdAtHomeTokenLifespan shouldBe 300_000L
        MdConstants.Login.redirectUri shouldBe "tachiyomisy://mangadex-auth"
        MdConstants.Login.clientId shouldBe "tachiyomisy"
        MdConstants.Login.authorizationCode shouldBe "authorization_code"
        MdConstants.Login.refreshToken shouldBe "refresh_token"
    }

    @Test
    fun authUrlCarriesS256Challenge() {
        // SHA-256("verifier") base64url without padding.
        val url = MdConstants.Login.authUrl("verifier")
        url shouldStartWith "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/auth?"
        url shouldBe "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/auth" +
            "?client_id=tachiyomisy&response_type=code&redirect_uri=tachiyomisy%3A%2F%2Fmangadex-auth" +
            "&code_challenge=iMnq5o6zALKXGivsnlom_0F5_WYda32GHkxlV7mq7hQ&code_challenge_method=S256"
    }
}
