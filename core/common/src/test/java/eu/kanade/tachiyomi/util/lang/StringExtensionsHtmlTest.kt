package eu.kanade.tachiyomi.util.lang

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** `htmlDecode` goes through `android.text.Html`, which only Robolectric implements. */
@RunWith(RobolectricTestRunner::class)
internal class StringExtensionsHtmlTest {
    @Test
    fun htmlDecodeStripsTags() {
        "a &amp; <b>b</b>".htmlDecode() shouldBe "a & b"
    }
}
