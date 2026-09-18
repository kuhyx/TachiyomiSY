package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Page list, image url and image download of [HttpSourcePages]. */
internal class HttpSourcePagesTest {
    private val harness = SourceHarness()
    private val echo = EchoHttpSource()
    private val bare = BareHttpSource()
    private val chapter = SChapter(name = "c1", url = "/chapter/1")
    private val page = Page(index = 0, url = "https://bare.example/page/1", imageUrl = "https://img.example/1.png")

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "page body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun pageListFetchesAndParses() = runTest {
        echo.getPageList(chapter).single().url shouldBe "page body"
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/chapter/1"
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun pageListParseDefaultThrows() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getPageList(chapter) }
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/chapter/1"
    }

    @Test
    fun imageUrlFetchesPageUrl() = runTest {
        echo.getImageUrl(page) shouldBe "page body"
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/page/1"
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun imageUrlParseDefaultThrows() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getImageUrl(page) }
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/page/1"
    }

    @Test
    fun imageUrlFailureIsHttpException() = runTest {
        harness.server.code = 404
        shouldThrow<HttpException> { echo.getImageUrl(page) }
    }

    @Test
    fun getImageDownloadsImageUrl() = runTest {
        val response = bare.getImage(page)
        response.body.string() shouldBe "page body"
        harness.server.requests.single().url.toString() shouldBe "https://img.example/1.png"
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun getImageWithExistingSize() = runTest {
        bare.getImage(page, existingSize = 1024L).body.string() shouldBe "page body"
        harness.server.requests.single().url.toString() shouldBe "https://img.example/1.png"
    }

    @Test
    fun getImageFailureIsHttpException() = runTest {
        harness.server.code = 500
        shouldThrow<HttpException> { bare.getImage(page) }
    }

    @Test
    fun imageRequestNeedsImageUrl() {
        val request = bare.invokeDeclared(HttpSourcePages::class, "imageRequest", listOf(page)) as Request
        request.url.toString() shouldBe "https://img.example/1.png"
        shouldThrow<NullPointerException> {
            bare.invokeDeclared(HttpSourcePages::class, "imageRequest", listOf(Page(index = 1, url = "u")))
        }
    }
}
