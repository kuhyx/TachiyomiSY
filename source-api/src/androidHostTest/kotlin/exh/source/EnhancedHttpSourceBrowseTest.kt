package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.BareHttpSource
import eu.kanade.tachiyomi.source.online.EchoHttpSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable

/** Source switching and listing forwards of [EnhancedHttpSourceBrowse]. */
internal class EnhancedHttpSourceBrowseTest {
    private val harness = SourceHarness()
    private val original = BareHttpSource(name = "Original", lang = "ja", baseUrl = "https://original.example")
    private val enhanced = EchoHttpSource()
    private val source = EnhancedHttpSource(original, enhanced)

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "enhanced body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun sourceFollowsPreference() {
        (source.source() === enhanced) shouldBe true
        harness.delegateSources = false
        (source.source() === original) shouldBe true
        (source.originalSource === original) shouldBe true
        (source.enhancedSource === enhanced) shouldBe true
    }

    @Test
    fun identityReadsActiveSource() {
        source.baseUrl shouldBe "https://bare.example"
        (source.headers === enhanced.headers) shouldBe true
        source.name shouldBe "Request Only"
        source.lang shouldBe "en"
        source.id shouldBe enhanced.id
        source.getHomeUrl() shouldBe "https://bare.example"
        source.toString() shouldBe "Request Only (EN)"
        harness.delegateSources = false
        source.baseUrl shouldBe "https://original.example"
        source.name shouldBe "Original"
        source.lang shouldBe "ja"
        source.id shouldBe original.id
        source.toString() shouldBe "Original (JA)"
    }

    @Test
    fun supportsLatestReadsActive() {
        source.supportsLatest shouldBe true
    }

    @Test
    fun clientAlwaysComesFromOriginal() {
        source.client.interceptors shouldBe original.client.interceptors
        harness.delegateSources = false
        source.client.interceptors shouldBe original.client.interceptors
    }

    @Test
    fun suspendListingsGoToEnhanced() = runTest {
        source.getPopularManga(1).mangas.single().title shouldBe "enhanced body"
        source.getSearchManga(2, "q", FilterList()).hasNextPage shouldBe false
        source.getLatestUpdates(3).mangas.single().title shouldBe "enhanced body"
        harness.server.requests.map { it.url.encodedPath } shouldBe listOf("/popular/1", "/search/2", "/latest/3")
    }

    @Test
    fun suspendListingsGoToOriginal() = runTest {
        harness.delegateSources = false
        shouldThrow<UnsupportedOperationException> { source.getPopularManga(1) }
        shouldThrow<UnsupportedOperationException> { source.getSearchManga(1, "q", FilterList()) }
        shouldThrow<UnsupportedOperationException> { source.getLatestUpdates(1) }
    }

    @Test
    fun rxListingsForwardToEnhanced() {
        source.blockingPage("fetchPopularManga", listOf(1)).hasNextPage shouldBe true
        source.blockingPage("fetchSearchManga", listOf(2, "q", FilterList())).hasNextPage shouldBe false
        source.blockingPage("fetchLatestUpdates", listOf(3)).mangas.single().title shouldBe "enhanced body"
    }

    @Test
    fun rxListingsForwardToOriginal() {
        harness.delegateSources = false
        shouldThrow<UnsupportedOperationException> { source.blockingPage("fetchPopularManga", listOf(1)) }
        shouldThrow<UnsupportedOperationException> { source.blockingPage("fetchLatestUpdates", listOf(1)) }
    }

    private fun EnhancedHttpSource.blockingPage(name: String, args: List<Any?>): MangasPage {
        val observable = invokeDeclared(EnhancedHttpSourceBrowse::class, name, args) as Observable<*>
        return observable.toBlocking().single() as MangasPage
    }
}
