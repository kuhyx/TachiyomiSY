package exh.metadata.metadata

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The URI helpers in the [LanraragiSearchMetadata] companion, plus JSON serialization. */
internal class LanraragiUriTest {
    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun firstPageThumbnailHasNoQuery() {
        val builder = stubUriBuilder("rendered")
        LanraragiSearchMetadata.getThumbnailUri(baseUrl = "https://lrr", id = "abc", page = 1) shouldBe "rendered"
        verify { Uri.parse("https://lrr/api/archives/abc/thumbnail") }
        verify(exactly = 0) { builder.appendQueryParameter(any(), any()) }
    }

    @Test
    fun laterPageThumbnailAddsQuery() {
        val builder = stubUriBuilder("rendered")
        LanraragiSearchMetadata.getThumbnailUri(baseUrl = "https://lrr", id = "abc", page = 3) shouldBe "rendered"
        verify { Uri.parse("https://lrr/api/archives/abc/thumbnail") }
        verify { builder.appendQueryParameter("page", "3") }
        verify { builder.appendQueryParameter("no_fallback", "true") }
    }

    @Test
    fun apiUriBuilderJoinsBaseAndPath() {
        val builder = stubUriBuilder("rendered")
        LanraragiSearchMetadata.getApiUriBuilder("https://lrr", "/api/search") shouldBe builder
        verify { Uri.parse("https://lrr/api/search") }
    }

    @Test
    fun jsonRoundTripsEveryField() {
        val meta = LanraragiSearchMetadata().apply {
            arcId = "abc"
            title = "Title"
            summary = "Summary"
            pageCount = 7
            baseUrl = "https://lrr"
            filename = "file"
            extension = "zip"
        }
        val encoded = Json.encodeToString(LanraragiSearchMetadata.serializer(), meta)
        val decoded = Json.decodeFromString(LanraragiSearchMetadata.serializer(), encoded)
        decoded.arcId shouldBe "abc"
        decoded.title shouldBe "Title"
        decoded.summary shouldBe "Summary"
        decoded.pageCount shouldBe 7
        decoded.baseUrl shouldBe "https://lrr"
        decoded.filename shouldBe "file"
        decoded.extension shouldBe "zip"
        decoded.url shouldBe "/reader?id=abc"
    }

    @Test
    fun jsonOmitsDefaults() {
        Json.encodeToString(LanraragiSearchMetadata.serializer(), LanraragiSearchMetadata()) shouldBe "{}"
        Json.decodeFromString(LanraragiSearchMetadata.serializer(), "{}").arcId shouldBe null
    }

    @Test
    fun jsonAcceptsExplicitNulls() {
        val text = """
            {"arcId":null,"title":null,"summary":null,"pageCount":null,"baseUrl":null,"filename":null,
             "extension":null}
        """.trimIndent()
        val decoded = Json.decodeFromString(LanraragiSearchMetadata.serializer(), text)
        decoded.arcId shouldBe null
        decoded.title shouldBe null
        decoded.summary shouldBe null
        decoded.pageCount shouldBe null
        decoded.baseUrl shouldBe null
        decoded.filename shouldBe null
        decoded.extension shouldBe null
    }
}
