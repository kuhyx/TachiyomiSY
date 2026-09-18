package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Details and chapter fetches of [HttpSourceManga], driven through the combined suspend update. */
internal class HttpSourceMangaTest {
    private val harness = SourceHarness()
    private val echo = EchoHttpSource()
    private val bare = BareHttpSource()
    private val manga = SManga(url = "/manga/1", title = "Original")
    private val existing = listOf(SChapter(name = "existing", url = "/c/0"))

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "manga body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun detailsFetchInitializes() = runTest {
        val update = echo.getMangaUpdate(manga = manga, chapters = existing, fetchDetails = true, fetchChapters = false)
        update.manga.title shouldBe "manga body"
        update.manga.initialized shouldBe true
        update.chapters shouldBe existing
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/manga/1"
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun chapterFetchParses() = runTest {
        val update = echo.getMangaUpdate(manga = manga, chapters = existing, fetchDetails = false, fetchChapters = true)
        (update.manga === manga) shouldBe true
        update.chapters.single().name shouldBe "manga body"
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/manga/1"
    }

    @Test
    fun bothFetchesRunTogether() = runTest {
        val update = echo.getMangaUpdate(manga = manga, chapters = existing, fetchDetails = true, fetchChapters = true)
        update.manga.title shouldBe "manga body"
        update.chapters.single().name shouldBe "manga body"
        harness.server.requests.size shouldBe 2
    }

    @Test
    fun detailsParseDefaultThrows() = runTest {
        shouldThrow<UnsupportedOperationException> {
            bare.getMangaUpdate(manga = manga, chapters = existing, fetchDetails = true, fetchChapters = false)
        }
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/manga/1"
    }

    @Test
    fun chapterParseDefaultThrows() = runTest {
        shouldThrow<UnsupportedOperationException> {
            bare.getMangaUpdate(manga = manga, chapters = existing, fetchDetails = false, fetchChapters = true)
        }
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun mangaDetailsRequestIsGet() {
        val request = bare.invokeDeclared(HttpSourceManga::class, "mangaDetailsRequest", listOf(manga)) as Request
        request.method shouldBe "GET"
        request.url.toString() shouldBe "https://bare.example/manga/1"
        request.header("User-Agent") shouldBe USER_AGENT
    }
}
