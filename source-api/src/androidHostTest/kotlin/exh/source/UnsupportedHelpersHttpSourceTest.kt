package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.BareHttpSource
import eu.kanade.tachiyomi.source.online.SourceHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Every helper override of [UnsupportedHelpersHttpSource] throws, whichever delegating source inherits it. */
internal class UnsupportedHelpersHttpSourceTest {
    private val harness = SourceHarness()
    private val source: UnsupportedHelpersHttpSource = EnhancedHttpSource(BareHttpSource(), BareHttpSource())
    private val response = cannedResponse("")

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun requestHelpersThrow() {
        assertNeverCalled("popularMangaRequest", listOf(1))
        assertNeverCalled("searchMangaRequest", listOf(1, "q", FilterList()))
        assertNeverCalled("latestUpdatesRequest", listOf(1))
    }

    @Test
    fun listingParseHelpersThrow() {
        assertNeverCalled("popularMangaParse", listOf(response))
        assertNeverCalled("searchMangaParse", listOf(response))
        assertNeverCalled("latestUpdatesParse", listOf(response))
    }

    @Test
    fun mangaParseHelpersThrow() {
        assertNeverCalled("mangaDetailsParse", listOf(response))
        assertNeverCalled("chapterListParse", listOf(response))
    }

    @Test
    fun pageParseHelpersThrow() {
        assertNeverCalled("pageListParse", listOf(response))
        assertNeverCalled("imageUrlParse", listOf(response))
    }

    private fun assertNeverCalled(name: String, args: List<Any?>) {
        withClue(name) {
            val error = shouldThrow<UnsupportedOperationException> {
                source.invokeDeclared(UnsupportedHelpersHttpSource::class, name, args)
            }
            error.message shouldBe "Should never be called!"
        }
    }
}
