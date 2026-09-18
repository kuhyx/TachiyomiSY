package exh.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
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

/** Page forwards of [EnhancedHttpSource] to whichever source is active. */
internal class EnhancedHttpSourceTest {
    private val harness = SourceHarness()
    private val original = BareHttpSource(name = "Original")
    private val enhanced = EchoHttpSource()
    private val source = EnhancedHttpSource(original, enhanced)
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
    fun pageListForwards() = runTest {
        source.getPageList(chapter).single().url shouldBe "page body"
        val rx = source.invokeDeclared(EnhancedHttpSource::class, "fetchPageList", listOf(chapter))
        ((rx as Observable<*>).toBlocking().single() as List<*>).size shouldBe 1
        harness.delegateSources = false
        shouldThrow<UnsupportedOperationException> { source.getPageList(chapter) }
    }

    @Test
    fun imageUrlForwards() = runTest {
        source.getImageUrl(page) shouldBe "page body"
        val rx = source.invokeDeclared(EnhancedHttpSource::class, "fetchImageUrl", listOf(page))
        (rx as Observable<*>).toBlocking().single() shouldBe "page body"
        harness.delegateSources = false
        shouldThrow<UnsupportedOperationException> { source.getImageUrl(page) }
    }

    @Test
    fun imageForwardsWithExistingSize() = runTest {
        source.getImage(page, 12L).body.string() shouldBe "page body"
        source.getImage(page).body.string() shouldBe "page body"
        harness.server.requests.map { it.url.toString() } shouldBe List(2) { "https://img.example/1.png" }
    }
}
