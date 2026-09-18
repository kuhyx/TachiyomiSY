package tachiyomi.data.release

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.model.Release

internal class ReleaseServiceImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val requested = mutableListOf<String>()

    // A service whose HTTP client answers every request with the given status and body, recording the url.
    private fun service(code: Int, body: String): ReleaseServiceImpl {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requested += chain.request().url.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("stubbed")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return ReleaseServiceImpl(mockk<NetworkHelper> { every { client } returns httpClient }, json)
    }

    @Test
    fun latestParsesTheGithubRelease() = runTest {
        val payload = """{"tag_name":"v2","body":"by @kuhy","html_url":"https://github.com/o/r/releases/tag/v2",""" +
            """"assets":[{"browser_download_url":"https://github.com/o/r/app.apk"}]}"""

        val release = service(code = 200, body = payload).latest("o/r")

        release shouldBe Release(
            version = "v2",
            info = "by [@kuhy](https://github.com/kuhy)",
            releaseLink = "https://github.com/o/r/releases/tag/v2",
            assets = listOf("https://github.com/o/r/app.apk"),
        )
        requested shouldBe listOf("https://api.github.com/repos/o/r/releases/latest")
    }

    @Test
    fun latestThrowsOnHttpError() = runTest {
        val error = shouldThrow<HttpException> { service(code = 404, body = "").latest("o/r") }

        error.code shouldBe 404
    }
}
