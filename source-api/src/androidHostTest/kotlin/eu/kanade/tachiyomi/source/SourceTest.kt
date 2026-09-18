package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.StubSource
import eu.kanade.tachiyomi.source.online.invokeDeclared
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Defaults of the [Source] interface: language, filters and the legacy Rx fetches. */
internal class SourceTest {
    private val source = StubSource()
    private val manga = SManga(url = "/m", title = "t")
    private val chapter = SChapter(name = "c", url = "/c")

    @Test
    fun langDefaultsToEmpty() {
        source.lang shouldBe ""
    }

    @Test
    fun identityFromImplementation() {
        source.id shouldBe 7L
        source.name shouldBe "Stub Source"
        source.supportsLatest shouldBe false
    }

    @Test
    fun filterListDefaultsToEmpty() {
        source.getFilterList().list shouldBe emptyList()
        source.getFilterList().size shouldBe 0
    }

    @Test
    fun fetchMangaDetailsDefaultThrows() {
        shouldThrow<UnsupportedOperationException> {
            source.invokeDeclared(Source::class, "fetchMangaDetails", listOf(manga))
        }
    }

    @Test
    fun fetchChapterListDefaultThrows() {
        shouldThrow<UnsupportedOperationException> {
            source.invokeDeclared(Source::class, "fetchChapterList", listOf(manga))
        }
    }

    @Test
    fun fetchPageListDefaultThrows() {
        shouldThrow<UnsupportedOperationException> {
            source.invokeDeclared(Source::class, "fetchPageList", listOf(chapter))
        }
    }

    @Test
    fun suspendApiIsImplemented() = runTest {
        source.getPopularManga(1).mangas shouldBe emptyList()
        source.getLatestUpdates(1).hasNextPage shouldBe false
        source.getSearchManga(1, "q", FilterList()).mangas shouldBe emptyList()
        source.getPageList(chapter) shouldBe emptyList()
        val update =
            source.getMangaUpdate(manga = manga, chapters = listOf(chapter), fetchDetails = true, fetchChapters = true)
        (update.manga === manga) shouldBe true
        update.chapters shouldBe listOf(chapter)
    }
}
