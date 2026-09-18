package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** URL helpers of [HttpSource]: domain stripping, manga/chapter urls and the chapter hook. */
internal class HttpSourceTest {
    private val harness = SourceHarness()
    private val source = BareHttpSource()

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun mangaUrlDropsSchemeAndHost() {
        source.mangaUrlWithoutDomain("https://bare.example/manga/1") shouldBe "/manga/1"
    }

    @Test
    fun mangaUrlKeepsQuery() {
        source.mangaUrlWithoutDomain("https://bare.example/manga?id=1") shouldBe "/manga?id=1"
    }

    @Test
    fun mangaUrlKeepsFragment() {
        source.mangaUrlWithoutDomain("https://bare.example/manga#top") shouldBe "/manga#top"
    }

    @Test
    fun mangaUrlKeepsQueryAndFragment() {
        source.mangaUrlWithoutDomain("https://bare.example/m?id=1#top") shouldBe "/m?id=1#top"
    }

    @Test
    fun mangaUrlEscapesSpaces() {
        source.mangaUrlWithoutDomain("https://bare.example/a b") shouldBe "/a b"
    }

    @Test
    fun mangaUrlKeepsInvalidUriAsIs() {
        source.mangaUrlWithoutDomain("https://bare.example/a|b") shouldBe "https://bare.example/a|b"
    }

    @Test
    fun chapterUrlDropsSchemeAndHost() {
        source.chapterUrlWithoutDomain("https://bare.example/chapter/1?p=2#x") shouldBe "/chapter/1?p=2#x"
        source.chapterUrlWithoutDomain("http://bare.example/^") shouldBe "http://bare.example/^"
    }

    @Test
    fun mangaUrlIsDetailsRequestUrl() {
        source.getMangaUrl(SManga(url = "/manga/9", title = "t")) shouldBe "https://bare.example/manga/9"
    }

    @Test
    fun chapterUrlIsPageListRequestUrl() {
        source.getChapterUrl(SChapter(name = "c", url = "/chapter/9")) shouldBe "https://bare.example/chapter/9"
    }

    @Test
    fun prepareNewChapterIsNoOp() {
        val chapter = SChapter(name = "c", url = "/chapter/9")
        val manga = SManga(url = "/manga/9", title = "t")
        source.invokeDeclared(HttpSource::class, "prepareNewChapter", listOf(chapter, manga)) shouldBe Unit
        chapter.name shouldBe "c"
        manga.title shouldBe "t"
    }
}
