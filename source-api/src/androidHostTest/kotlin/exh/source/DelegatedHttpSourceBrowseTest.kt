package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.EchoHttpSource
import eu.kanade.tachiyomi.source.online.PlainDelegatedSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.StubDelegatedSource
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable

/** Identity and listing forwards of [DelegatedHttpSourceBrowse], plus the compatibility check. */
internal class DelegatedHttpSourceBrowseTest {
    private val harness = SourceHarness()
    private val inner = EchoHttpSource()
    private val delegated = StubDelegatedSource(delegate = inner)

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "delegated body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun identityReadsDelegate() {
        val delegated = PlainDelegatedSource(inner)
        delegated.lang shouldBe "en"
        delegated.baseUrl shouldBe "https://bare.example"
        (delegated.headers === inner.headers) shouldBe true
        delegated.supportsLatest shouldBe true
        delegated.name shouldBe "Request Only"
        delegated.id shouldBe inner.id
        delegated.getHomeUrl() shouldBe "https://bare.example"
        delegated.toString() shouldBe "Request Only (EN)"
        (delegated.delegate === inner) shouldBe true
    }

    @Test
    fun clientsFromDelegateAndNetwork() {
        val delegated = PlainDelegatedSource(inner)
        delegated.client.interceptors shouldBe harness.server.client.interceptors
        delegated.baseHttpClient shouldBe null
        delegated.networkHttpClient.interceptors shouldBe harness.server.client.interceptors
    }

    @Test
    fun suspendListingsForward() = runTest {
        delegated.getPopularManga(1).mangas.single().title shouldBe "delegated body"
        delegated.getSearchManga(2, "q", FilterList()).hasNextPage shouldBe false
        delegated.getLatestUpdates(3).mangas.single().title shouldBe "delegated body"
        harness.server.requests.map { it.url.encodedPath } shouldBe listOf("/popular/1", "/search/2", "/latest/3")
    }

    @Test
    fun rxListingsForward() {
        delegated.blockingPage("fetchPopularManga", listOf(1)).hasNextPage shouldBe true
        delegated.blockingPage("fetchSearchManga", listOf(2, "q", FilterList())).hasNextPage shouldBe false
        delegated.blockingPage("fetchLatestUpdates", listOf(3)).mangas.single().title shouldBe "delegated body"
        harness.server.requests.map { it.url.encodedPath } shouldBe listOf("/popular/1", "/search/2", "/latest/3")
    }

    @Test
    fun versionMismatchIsIncompatible() = runTest {
        val mismatched = StubDelegatedSource(delegate = inner, versionId = 2)
        val error = shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getPopularManga(1) }
        error.message shouldBe "Delegate source is not compatible (versionId: 2 <=> 1, lang: en <=> en)!"
        harness.server.requests shouldBe emptyList()
    }

    @Test
    fun langMismatchIsIncompatible() = runTest {
        val mismatched = StubDelegatedSource(delegate = inner, lang = "ja")
        val error = shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getLatestUpdates(1) }
        error.message shouldBe "Delegate source is not compatible (versionId: 1 <=> 1, lang: ja <=> en)!"
    }

    @Test
    fun rxListingsCheckCompatibility() {
        val mismatched = StubDelegatedSource(delegate = inner, versionId = 3)
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceBrowse::class, "fetchPopularManga", listOf(1))
        }
        val searchArgs = listOf(1, "q", FilterList())
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceBrowse::class, "fetchSearchManga", searchArgs)
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSourceBrowse::class, "fetchLatestUpdates", listOf(1))
        }
    }

    @Test
    fun latestHelpersReachDelegate() {
        val request = delegated.invokeDeclared(DelegatedHttpSource::class, "delegateLatestUpdatesRequest", listOf(4))
        (request as Request).url.encodedPath shouldBe "/latest/4"
        val response = cannedResponse("parsed by delegate")
        val page = delegated.invokeDeclared(DelegatedHttpSource::class, "delegateLatestUpdatesParse", listOf(response))
        (page as MangasPage).mangas.single().title shouldBe "parsed by delegate"
        val mismatched = StubDelegatedSource(delegate = inner, versionId = 3)
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSource::class, "delegateLatestUpdatesRequest", listOf(4))
        }
    }

    @Test
    fun suspendListingsCheckDelegate() = runTest {
        val mismatched = StubDelegatedSource(delegate = inner, versionId = 3)
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.getSearchManga(1, "q", FilterList())
        }
    }

    private fun StubDelegatedSource.blockingPage(name: String, args: List<Any?>): MangasPage {
        val observable = invokeDeclared(DelegatedHttpSourceBrowse::class, name, args) as Observable<*>
        return observable.toBlocking().single() as MangasPage
    }
}
