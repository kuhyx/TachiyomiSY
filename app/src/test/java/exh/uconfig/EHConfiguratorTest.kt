package exh.uconfig

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.log.maybeInjectEHLogger
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.source.service.SourceManager

private const val PROFILE = "TachiyomiEH App"

/** An in-process e-hentai: hath perks page, settings page and profile actions. */
private class FakeEh(private val perks: String, private var profiles: Map<Int, String>) : Interceptor {
    val posted = mutableListOf<Map<String, String>>()
    var createCookies: List<String> = emptyList()

    private fun uconfigPage() = profiles.entries.joinToString(
        prefix = "<html><body><select name=\"profile_set\">",
        postfix = "</select></body></html>",
        separator = "",
    ) { (id, name) -> "<option value=\"$id\">$name</option>" }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
        val page = when {
            request.url.toString().contains("hathperks") -> perks
            request.method == "GET" -> uconfigPage()
            else -> applyAction(request.body!!, builder)
        }
        return builder.body(page.toResponseBody("text/html".toMediaType())).build()
    }

    private fun applyAction(body: RequestBody, builder: Response.Builder): String {
        val encoded = Buffer().also { body.writeTo(it) }.readUtf8()
        val form = encoded.split("&").associate { it.substringBefore("=") to it.substringAfter("=").replace("+", " ") }
        posted += form
        when (form["profile_action"]) {
            "delete" -> {
                profiles = profiles - form.getValue("profile_set").toInt()
            }
            "create" -> {
                profiles = profiles + (form.getValue("profile_set").toInt() to form.getValue("profile_name"))
                createCookies.forEach { builder.addHeader("Set-Cookie", it) }
            }
        }
        return uconfigPage()
    }
}

@RunWith(RobolectricTestRunner::class)
internal class EHConfiguratorTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val preferences = ExhPreferences(InMemoryPreferenceStore())
    private val sourceManager = mockk<SourceManager>()

    // A row with a purchase form is not bought yet; the last row of a perk wins.
    private val perks = """
        <table class="stuffbox">
        ${perkRows(bought = false)}
        ${perkRows(bought = true)}
        <tr><td>Something Else</td><td>x</td><td></td></tr>
        <tr><td>More Thumbs</td><td>x</td><td></td></tr>
        <tr><td>Thumbs Up</td><td>x</td><td><form></form></td></tr>
        <tr><td>All Thumbs</td><td>x</td><td></td></tr>
        <tr><td>Paging Enlargement I</td><td>x</td><td></td></tr>
        <tr><td>Paging Enlargement II</td><td>x</td><td></td></tr>
        <tr><td>Paging Enlargement III</td><td>x</td><td><form></form></td></tr>
        </table>
    """.trimIndent()

    private fun perkRows(bought: Boolean) = listOf(
        "More Thumbs", "Thumbs Up", "All Thumbs",
        "Paging Enlargement I", "Paging Enlargement II", "Paging Enlargement III",
    ).joinToString("\n") { name ->
        "<tr><td>$name</td><td>x</td><td>${if (bought) "" else "<form></form>"}</td></tr>"
    }

    @Before
    fun setUp() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        stopKoin()
        startKoin {
            modules(
                module {
                    single { preferences }
                    single<SourceManager> { sourceManager }
                },
            )
        }
        mockkStatic("exh.log.EHNetworkLoggingKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun serve(fake: FakeEh) {
        every { any<OkHttpClient.Builder>().maybeInjectEHLogger() } answers {
            firstArg<OkHttpClient.Builder>().addInterceptor(fake)
        }
    }

    private fun source(isExh: Boolean): EHentai = mockk {
        every { exh } returns isExh
        every { baseUrl } returns if (isExh) "https://exh.test" else "https://eh.test"
        every { name } returns if (isExh) "ExHentai" else "E-Hentai"
        every { exhPreferences } returns preferences
    }

    @Test
    fun replacesProfileStoresCookies() = runBlocking<Unit> {
        every { sourceManager.get(EH_SOURCE_ID) } returns source(isExh = false)
        every { sourceManager.get(EXH_SOURCE_ID) } returns source(isExh = true)
        val fake = FakeEh(perks, mapOf(1 to "Default", 2 to PROFILE))
        fake.createCookies = listOf("sk=key123; path=/", "s=sess; path=/", "hath_perks=hp; path=/", "other=1")
        serve(fake)
        EHConfigurator(context).configureAll()
        // Both sites share the account, so the profile the first site created is replaced again.
        fake.posted.map { it["profile_action"] } shouldContainExactly
            listOf("delete", "create", null, "delete", "create", null)
        fake.posted[1]["profile_set"] shouldBe "2"
        fake.posted[2]["tr"] shouldBe "3"
        fake.posted[2]["rc"] shouldBe "2"
        fake.posted[4]["profile_set"] shouldBe "2"
        preferences.ehSettingsProfile.get() shouldBe 2
        preferences.exhSettingsProfile.get() shouldBe 2
        preferences.exhSettingsKey.get() shouldBe "key123"
        preferences.exhSessionCookie.get() shouldBe "sess"
        preferences.exhHathPerksCookies.get() shouldBe "hp"
    }

    @Test
    fun missingCookiesKeepPrefs() = runBlocking<Unit> {
        every { sourceManager.get(EH_SOURCE_ID) } returns source(isExh = false)
        every { sourceManager.get(EXH_SOURCE_ID) } returns source(isExh = true)
        preferences.enableExhentai.set(true)
        preferences.exhSettingsKey.set("old")
        val fake = FakeEh("<html></html>", emptyMap())
        fake.createCookies = listOf("other=1")
        serve(fake)
        EHConfigurator(context).configureAll()
        preferences.exhSettingsKey.get() shouldBe "old"
        preferences.exhSessionCookie.get() shouldBe ""
        fake.posted.first { "profile_action" !in it }["tr"] shouldBe "0"
    }

    @Test
    fun noFreeSlotFails() = runBlocking<Unit> {
        every { sourceManager.get(EH_SOURCE_ID) } returns source(isExh = false)
        every { sourceManager.get(EXH_SOURCE_ID) } returns source(isExh = true)
        serve(FakeEh(perks, mapOf(1 to "a", 2 to "b", 3 to "c")))
        val error = shouldThrow<IllegalStateException> { EHConfigurator(context).configureAll() }
        error.message.orEmpty() shouldContain "E-Hentai"
    }
}
