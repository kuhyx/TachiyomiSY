package tachiyomi.core.common

import android.net.Uri
import android.util.Base64
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Proves the Robolectric + vintage wiring: android.jar behaves, not `Stub!`. */
@RunWith(RobolectricTestRunner::class)
internal class RobolectricSmokeTest {
    @Test
    fun uriAndBase64AreReal() {
        Uri.parse("https://example.org/a?b=c").getQueryParameter("b") shouldBe "c"
        Base64.encodeToString("hi".toByteArray(), Base64.NO_WRAP) shouldBe "aGk="
    }
}
