package eu.kanade.tachiyomi.util

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.Base64

internal class PkceUtilTest {

    @Test
    fun verifiersAreRandomUrlSafe() {
        val verifier = PkceUtil.generateCodeVerifier()
        verifier.length shouldBe 67
        verifier shouldNotContain "="
        verifier shouldNotContain "+"
        verifier shouldNotBe PkceUtil.generateCodeVerifier()
    }

    @Test
    fun challengeIsTheSha256OfThe() {
        val codes = PkceUtil.generateS256Codes()
        val digest = MessageDigest.getInstance("SHA-256").digest(codes.codeVerifier.toByteArray())
        codes.codeChallenge shouldBe Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        codes shouldBe PkceCodes(codeVerifier = codes.codeVerifier, codeChallenge = codes.codeChallenge)
        codes.copy(codeChallenge = "x").codeChallenge shouldBe "x"
    }
}
