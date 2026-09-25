package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EHentaiSearchUrlsTest {
    private fun jumpOrSeek(value: String): String =
        Uri.parse("https://e-hentai.org/").buildUpon().apply { appendJumpOrSeek(value) }.build().toString()

    @Test
    fun seekDates() {
        jumpOrSeek("2020-01-02") shouldBe "https://e-hentai.org/?seek=2020-01-02"
        jumpOrSeek("20-1") shouldBe "https://e-hentai.org/?seek=20-1"
        jumpOrSeek("2020") shouldBe "https://e-hentai.org/?seek=2020"
        jumpOrSeek("2007") shouldBe "https://e-hentai.org/?seek=2007"
        jumpOrSeek("2099") shouldBe "https://e-hentai.org/?seek=2099"
    }

    @Test
    fun jumpCounts() {
        jumpOrSeek("5") shouldBe "https://e-hentai.org/?jump=5"
        jumpOrSeek("5d") shouldBe "https://e-hentai.org/?jump=5d"
        jumpOrSeek("2w") shouldBe "https://e-hentai.org/?jump=2w"
        jumpOrSeek("3m") shouldBe "https://e-hentai.org/?jump=3m"
        jumpOrSeek("1y") shouldBe "https://e-hentai.org/?jump=1y"
        jumpOrSeek("7-") shouldBe "https://e-hentai.org/?jump=7-"
    }

    @Test
    fun yearsOutOfRangeAreJumps() {
        jumpOrSeek("2006") shouldBe "https://e-hentai.org/?jump=2006"
        jumpOrSeek("2100") shouldBe "https://e-hentai.org/?jump=2100"
    }

    @Test
    fun nonsenseIsIgnored() {
        jumpOrSeek("abc") shouldBe "https://e-hentai.org/"
        jumpOrSeek("") shouldBe "https://e-hentai.org/"
        jumpOrSeek("12x") shouldBe "https://e-hentai.org/"
    }

    @Test
    fun toplistUrls() {
        toplistUrl(ToplistOption.ALL_TIME, 1) shouldBe "https://e-hentai.org/toplist.php?tl=11&p=0"
        toplistUrl(ToplistOption.YESTERDAY, 3) shouldBe "https://e-hentai.org/toplist.php?tl=15&p=2"
    }
}
