package exh.md.utils

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MdTitlesTest {
    private val harness = SourceTestHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun cleanDescriptionStripsMarkdown() {
        MdUtil.cleanDescription("&amp; [link](http://x) **bold** _italic_ \n---\nfooter") shouldBe
            "& link bold italic"
    }

    @Test
    fun titleDirectLangWins() {
        val attrs = mangaAttributes(title = mapOf("en" to "English", "ja" to "日本"))
        MdUtil.getTitleFromManga(attrs, "en", true) shouldBe "English"
    }

    @Test
    fun titlePrefersExtensionLang() {
        val attrs = mangaAttributes(
            title = mapOf("ja" to "日本"),
            altTitles = listOf(mapOf("fr" to "Français"), mapOf("en" to "English")),
        )
        MdUtil.getTitleFromManga(attrs, "fr", true) shouldBe "Français"
        MdUtil.getTitleFromManga(attrs, "fr", false) shouldBe "日本"
    }

    @Test
    fun titleFallsBackEnThenOriginal() {
        val alt = listOf(mapOf("en" to "English"), mapOf("ja-ro" to "Romaji"), mapOf("ja" to "日本"))
        MdUtil.getTitleFromManga(mangaAttributes(title = emptyMap(), altTitles = alt), "de", true) shouldBe "English"
        val noEn = listOf(mapOf("ja" to "日本"), mapOf("ja-ro" to "Romaji"))
        MdUtil.getTitleFromManga(mangaAttributes(title = emptyMap(), altTitles = noEn), "de", false) shouldBe "Romaji"
        val onlyOriginal = listOf(mapOf("ja" to "日本"))
        MdUtil.getTitleFromManga(mangaAttributes(title = emptyMap(), altTitles = onlyOriginal), "de", false) shouldBe
            "日本"
        MdUtil.getTitleFromManga(mangaAttributes(title = emptyMap()), "de", false) shouldBe ""
    }

    @Test
    fun fromLangMap() {
        MdUtil.getFromLangMap(mapOf("de" to "D", "en" to "E"), "de", "ja") shouldBe "D"
        MdUtil.getFromLangMap(mapOf("en" to "E"), "de", "ja") shouldBe "E"
        MdUtil.getFromLangMap(mapOf("ja-ro" to "R"), "de", "ja") shouldBe "R"
        MdUtil.getFromLangMap(mapOf("jp-ro" to "J"), "de", "ja") shouldBe "J"
        MdUtil.getFromLangMap(emptyMap(), "de", "ja").shouldBeNull()
        MdUtil.getFromLangMap(mapOf("ja-ro" to "R"), "de", "ko").shouldBeNull()
    }

    @Test
    fun findTitleInMaps() {
        MdUtil.findTitleInMaps("en", mapOf("en" to "T"), emptyList()) shouldBe "T"
        MdUtil.findTitleInMaps("en", emptyMap(), listOf(mapOf("ja" to "J"), mapOf("en" to "A"))) shouldBe "A"
        MdUtil.findTitleInMaps("en", emptyMap(), emptyList()).shouldBeNull()
    }

    @Test
    fun altTitlesInDescription() {
        MdUtil.addAltTitleToDesc("desc", null) shouldBe "desc"
        MdUtil.addAltTitleToDesc("desc", emptyList()) shouldBe "desc"
        MdUtil.addAltTitleToDesc("desc", listOf("A", "B &amp; C")) shouldBe
            """
                desc

                Alternative titles:
                • A
                • B & C
            """.trimIndent()
        MdUtil.addAltTitleToDesc("", listOf("A")) shouldBe
            """
                Alternative titles:
                • A
            """.trimIndent()
    }

    @Test
    fun finalChapterInDescription() {
        MdUtil.addFinalChapterToDesc("desc", null, null) shouldBe "desc"
        MdUtil.addFinalChapterToDesc("desc", "", "") shouldBe "desc"
        MdUtil.addFinalChapterToDesc("desc", "3", "12") shouldBe
            """
                desc

                Final chapter:
                Vol.3 Ch.12
            """.trimIndent()
        MdUtil.addFinalChapterToDesc("", null, "12") shouldBe
            """
                Final chapter:
                Ch.12
            """.trimIndent()
        MdUtil.addFinalChapterToDesc(" ", "3", null) shouldBe
            """
                 Final chapter:
                Vol.3
            """.trimIndent()
    }
}
