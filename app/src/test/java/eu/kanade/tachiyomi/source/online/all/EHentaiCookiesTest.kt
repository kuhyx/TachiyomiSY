package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.ui.login.EhLoginActivity
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EHentaiCookiesTest {
    private val harness = SourceTestHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    private fun source(exh: Boolean) = EHentai(if (exh) EXH_SOURCE_ID else EH_SOURCE_ID, exh, harness.application)

    @Test
    fun spPrefPicksSiteProfile() {
        harness.exhPreferences.ehSettingsProfile.set(3)
        harness.exhPreferences.exhSettingsProfile.set(7)
        source(exh = false).spPref().get() shouldBe 3
        source(exh = true).spPref().get() shouldBe 7
    }

    @Test
    fun rawCookiesWithoutExhentai() {
        source(exh = false).rawCookies(1) shouldContainExactly mapOf("sl" to "dm_2", "nw" to "1")
    }

    @Test
    fun rawCookiesWithExhentaiMinimal() {
        harness.exhPreferences.enableExhentai.set(true)
        harness.exhPreferences.memberIdVal.set("m")
        harness.exhPreferences.passHashVal.set("p")
        harness.exhPreferences.igneousVal.set("i")
        val cookies = source(exh = true).rawCookies(2)
        cookies[EhLoginActivity.MEMBER_ID_COOKIE] shouldBe "m"
        cookies[EhLoginActivity.PASS_HASH_COOKIE] shouldBe "p"
        cookies[EhLoginActivity.IGNEOUS_COOKIE] shouldBe "i"
        cookies["sp"] shouldBe "2"
        cookies shouldNotContainKey "sk"
        cookies shouldNotContainKey "s"
        cookies shouldNotContainKey "hath_perks"
    }

    @Test
    fun rawCookiesWithExhentaiSession() {
        harness.exhPreferences.enableExhentai.set(true)
        harness.exhPreferences.exhSettingsKey.set("key")
        harness.exhPreferences.exhSessionCookie.set("sess")
        harness.exhPreferences.exhHathPerksCookies.set("perks")
        val cookies = source(exh = true).rawCookies(0)
        cookies["sk"] shouldBe "key"
        cookies["s"] shouldBe "sess"
        cookies["hath_perks"] shouldBe "perks"
    }

    @Test
    fun cookiesHeaderEncodes() {
        harness.exhPreferences.ehSettingsProfile.set(4)
        harness.exhPreferences.enableExhentai.set(true)
        harness.exhPreferences.memberIdVal.set("a b")
        val source = source(exh = false)
        source.cookiesHeader() shouldBe
            "ipb_member_id=a+b; ipb_pass_hash=; igneous=; sp=4; sl=dm_2; nw=1"
        source.cookiesHeader(9) shouldBe
            "ipb_member_id=a+b; ipb_pass_hash=; igneous=; sp=9; sl=dm_2; nw=1"
    }

    @Test
    fun addParamAppendsQuery() {
        source(exh = false).addParam("https://e-hentai.org/?a=1", "next", "5") shouldBe
            "https://e-hentai.org/?a=1&next=5"
    }

    @Test
    fun clientSendsCookieHeader() {
        harness.enqueue("<html></html>")
        val source = source(exh = false)
        source.client.newCall(source.exGet("https://e-hentai.org/x")).execute().close()
        harness.takeRequest().headers["Cookie"] shouldBe "sl=dm_2; nw=1"
    }
}
