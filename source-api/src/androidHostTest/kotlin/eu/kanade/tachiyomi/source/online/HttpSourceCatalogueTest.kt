package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Listing fetches of [HttpSourceCatalogue]: each Rx fetch and the throwing request/parse defaults. */
internal class HttpSourceCatalogueTest {
    private val harness = SourceHarness()
    private val echo = EchoHttpSource()
    private val requestOnly = RequestOnlyHttpSource()
    private val bare = BareHttpSource()

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "listing body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun popularFetchesAndParses() = runTest {
        val page = echo.getPopularManga(2)
        page.mangas.single().title shouldBe "listing body"
        page.hasNextPage shouldBe true
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/popular/2"
        harness.server.requests.single().header("User-Agent") shouldBe USER_AGENT
    }

    @Test
    fun searchFetchesAndParses() = runTest {
        val page = echo.getSearchManga(3, "query", FilterList())
        page.mangas.single().title shouldBe "listing body"
        page.hasNextPage shouldBe false
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/search/3?q=query&filters=0"
    }

    @Test
    fun latestFetchesAndParses() = runTest {
        val page = echo.getLatestUpdates(4)
        page.mangas.single().title shouldBe "listing body"
        harness.server.requests.single().url.toString() shouldBe "https://bare.example/latest/4"
    }

    @Test
    fun failedResponseIsHttpException() = runTest {
        harness.server.code = 503
        shouldThrow<HttpException> { echo.getPopularManga(1) }
    }

    @Test
    fun popularDefaultsThrow() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getPopularManga(1) }
        shouldThrow<UnsupportedOperationException> { requestOnly.getPopularManga(1) }
        harness.server.requests.size shouldBe 1
    }

    @Test
    fun searchDefaultsThrow() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getSearchManga(1, "q", FilterList()) }
        shouldThrow<UnsupportedOperationException> { requestOnly.getSearchManga(1, "q", FilterList()) }
        harness.server.requests.size shouldBe 1
    }

    @Test
    fun latestDefaultsThrow() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getLatestUpdates(1) }
        shouldThrow<UnsupportedOperationException> { requestOnly.getLatestUpdates(1) }
        harness.server.requests.size shouldBe 1
    }
}
