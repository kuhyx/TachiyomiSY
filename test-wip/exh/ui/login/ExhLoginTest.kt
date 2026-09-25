package exh.ui.login

import android.webkit.CookieManager
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.HttpCookie

private const val URL = "https://forums.e-hentai.org/"

@RunWith(RobolectricTestRunner::class)
internal class ExhLoginTest {
    private fun cookie(name: String, value: String): HttpCookie = HttpCookie(name, value)

    @Test
    fun noCookiesForAnUnknownUrl() {
        cookiesFor("https://nothing.invalid/").shouldBeNull()
    }

    @Test
    fun cookiesAreParsedFromTheManager() {
        CookieManager.getInstance().setCookie(URL, "ipb_member_id=12")
        CookieManager.getInstance().setCookie(URL, "ipb_pass_hash=hash")
        val parsed = cookiesFor(URL)
        parsed?.map { it.name }?.sorted() shouldBe listOf("ipb_member_id", "ipb_pass_hash")
        parsed?.single { it.name == "ipb_member_id" }?.value shouldBe "12"
    }

    @Test
    fun forumLoginNeedsBothCookies() {
        listOf(cookie("ipb_member_id", "12"), cookie("ipb_pass_hash", "hash")).hasForumLogin() shouldBe true
        listOf(cookie("IPB_MEMBER_ID", "12"), cookie("IPB_PASS_HASH", "hash")).hasForumLogin() shouldBe true
        listOf(cookie("ipb_member_id", "12"), cookie("ipb_pass_hash", "")).hasForumLogin() shouldBe false
        listOf(cookie("ipb_member_id", "12"), cookie("igneous", "ig")).hasForumLogin() shouldBe false
        emptyList<HttpCookie>().hasForumLogin() shouldBe false
    }

    @Test
    fun loginNeedsEveryCookie() {
        val full = listOf(
            cookie("ipb_member_id", "12"),
            cookie("ipb_pass_hash", "hash"),
            cookie("igneous", "site"),
        )
        full.toExhLogin(customIgneous = null) shouldBe ExhLogin(memberId = "12", passHash = "hash", igneous = "site")
        full.toExhLogin(customIgneous = "mine")?.igneous shouldBe "mine"
        full.drop(1).toExhLogin(customIgneous = null).shouldBeNull()
        listOf(full[0], full[2]).toExhLogin(customIgneous = null).shouldBeNull()
        full.dropLast(1).toExhLogin(customIgneous = null).shouldBeNull()
    }

    @Test
    fun cookieNamesAreLowercased() {
        val login = listOf(
            cookie("IPB_MEMBER_ID", "7"),
            cookie("IPB_PASS_HASH", "h"),
            cookie("IGNEOUS", "i"),
        ).toExhLogin(customIgneous = null)
        login?.memberId shouldBe "7"
    }

    @Test
    fun loginDataClass() {
        val login = ExhLogin(memberId = "1", passHash = "2", igneous = "3")
        login.copy(igneous = "4").igneous shouldBe "4"
        (login == ExhLogin(memberId = "1", passHash = "2", igneous = "3")) shouldBe true
        login.hashCode() shouldBe ExhLogin(memberId = "1", passHash = "2", igneous = "3").hashCode()
        login.toString().contains("ExhLogin") shouldBe true
        login.memberId shouldBe "1"
        login.passHash shouldBe "2"
    }
}
