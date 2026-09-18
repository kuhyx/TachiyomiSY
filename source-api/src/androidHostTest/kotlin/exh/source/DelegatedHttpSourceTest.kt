package exh.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.EchoHttpSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.StubDelegatedSource
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Observable

/** Page forwards of [DelegatedHttpSource], the delegate binding its constructor performs and its exception. */
internal class DelegatedHttpSourceTest {
    private val harness = SourceHarness()
    private val inner = EchoHttpSource()
    private val delegated = StubDelegatedSource(delegate = inner)
    private val chapter = SChapter(name = "c", url = "/chapter/1")
    private val page = Page(index = 0, url = "https://bare.example/page/1", imageUrl = "https://img.example/1.png")

    @BeforeEach
    fun setUp() {
        harness.install()
        harness.server.body = "page body"
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun constructorBindsToDelegate() {
        val routed = OkHttpClient()
        val source = EchoHttpSource()
        StubDelegatedSource(delegate = source, baseHttpClient = routed)
        (source.client === routed) shouldBe true
    }

    @Test
    fun pageListForwards() = runTest {
        delegated.getPageList(chapter).single().url shouldBe "page body"
        val rx = delegated.invokeDeclared(DelegatedHttpSource::class, "fetchPageList", listOf(chapter))
        ((rx as Observable<*>).toBlocking().single() as List<*>).size shouldBe 1
        harness.server.requests.map { it.url.encodedPath } shouldBe listOf("/chapter/1", "/chapter/1")
    }

    @Test
    fun imageUrlForwards() = runTest {
        delegated.getImageUrl(page) shouldBe "page body"
        val rx = delegated.invokeDeclared(DelegatedHttpSource::class, "fetchImageUrl", listOf(page))
        (rx as Observable<*>).toBlocking().single() shouldBe "page body"
        harness.server.requests.map { it.url.encodedPath } shouldBe listOf("/page/1", "/page/1")
    }

    @Test
    fun imageForwardsWithExistingSize() = runTest {
        delegated.getImage(page, 12L).body.string() shouldBe "page body"
        delegated.getImage(page).body.string() shouldBe "page body"
        harness.server.requests.map { it.url.toString() } shouldBe List(2) { "https://img.example/1.png" }
    }

    @Test
    fun forwardsCheckCompatibility() = runTest {
        val mismatched = StubDelegatedSource(delegate = inner, lang = "de")
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getPageList(chapter) }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getImageUrl(page) }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> { mismatched.getImage(page) }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSource::class, "fetchPageList", listOf(chapter))
        }
        shouldThrow<DelegatedHttpSource.IncompatibleDelegateException> {
            mismatched.invokeDeclared(DelegatedHttpSource::class, "fetchImageUrl", listOf(page))
        }
        harness.server.requests shouldBe emptyList()
    }

    @Test
    fun incompatibleExceptionMessage() {
        val error: RuntimeException = DelegatedHttpSource.IncompatibleDelegateException("why")
        error.message shouldBe "why"
        error.cause shouldBe null
    }
}
