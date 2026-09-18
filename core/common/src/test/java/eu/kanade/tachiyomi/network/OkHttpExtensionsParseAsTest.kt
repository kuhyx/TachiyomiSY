package eu.kanade.tachiyomi.network

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class OkHttpExtensionsParseAsTest {
    private val json = Json

    @Test
    fun parsesBodyAsRequestedType() {
        val response = cannedResponse(GET(TEST_URL), body = """{"title":"T","pages":3}""")
        val page = with(json) { response.parseAs<Page>() }
        page shouldBe Page(title = "T", pages = 3)
    }

    @Test
    fun parsesCollections() {
        val response = cannedResponse(GET(TEST_URL), body = "[1, 2]")
        with(json) { response.parseAs<List<Int>>() } shouldBe listOf(1, 2)
    }

    @Test
    fun parsesWithAnExplicitJson() {
        val response = cannedResponse(GET(TEST_URL), body = """{"title":"E","pages":1}""")
        response.parseAs<Page>(json) shouldBe Page(title = "E", pages = 1)
    }

    @Test
    fun parseAsIsInlineOnly() {
        // A reified inline function cannot be invoked as a plain method; the compiled body guards
        // against that, and reaching it here is the only way to execute it outside inlining.
        val response = cannedResponse(GET(TEST_URL))
        shouldThrow<UnsupportedOperationException> {
            invokeStatic("eu.kanade.tachiyomi.network.ParseAsContextualKt", "parseAs", listOf(json, response))
        }
        shouldThrow<UnsupportedOperationException> {
            invokeStatic(EXTENSIONS, "parseAsReified", listOf(response, json))
        }
    }

    // Internal, not private: parseAs resolves the serializer at runtime through reflection on the class.
    @Serializable
    internal data class Page(val title: String, val pages: Int)

    private companion object {
        const val EXTENSIONS = "eu.kanade.tachiyomi.network.OkHttpExtensionsKt"
    }
}
