package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.RxCatalogueSource
import eu.kanade.tachiyomi.source.online.StubCatalogueSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** The suspend wrappers of [CatalogueSource] over the legacy Rx API, and the throwing Rx defaults. */
internal class CatalogueSourceTest {
    private val details = SManga(url = "/m", title = "fetched")
    private val fetchedChapters = listOf(SChapter(name = "fetched", url = "/c/1"))
    private val pages = listOf(Page(index = 0, url = "/p/0"))
    private val rx = RxCatalogueSource(details = details, chapters = fetchedChapters, pages = pages)
    private val bare = StubCatalogueSource()
    private val manga = SManga(url = "/m", title = "stored")
    private val stored = listOf(SChapter(name = "stored", url = "/c/0"))

    @Test
    fun popularAwaitsRxFetch() = runTest {
        (rx.getPopularManga(1) === rx.listing) shouldBe true
    }

    @Test
    fun latestAwaitsRxFetch() = runTest {
        (rx.getLatestUpdates(1) === rx.listing) shouldBe true
    }

    @Test
    fun searchAwaitsRxFetch() = runTest {
        (rx.getSearchManga(1, "q", FilterList()) === rx.listing) shouldBe true
    }

    @Test
    fun pageListAwaitsRxFetch() = runTest {
        rx.getPageList(stored.single()) shouldBe pages
    }

    @Test
    fun mangaUpdateFetchesNothing() = runTest {
        val update = rx.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = false, fetchChapters = false)
        (update.manga === manga) shouldBe true
        update.chapters shouldBe stored
    }

    @Test
    fun mangaUpdateFetchesDetailsOnly() = runTest {
        val update = rx.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = true, fetchChapters = false)
        (update.manga === details) shouldBe true
        update.chapters shouldBe stored
    }

    @Test
    fun mangaUpdateFetchesChaptersOnly() = runTest {
        val update = rx.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = false, fetchChapters = true)
        (update.manga === manga) shouldBe true
        update.chapters shouldBe fetchedChapters
    }

    @Test
    fun mangaUpdateFetchesBoth() = runTest {
        val update = rx.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = true, fetchChapters = true)
        (update.manga === details) shouldBe true
        update.chapters shouldBe fetchedChapters
    }

    @Test
    fun listingDefaultsThrow() = runTest {
        shouldThrow<UnsupportedOperationException> { bare.getPopularManga(1) }
        shouldThrow<UnsupportedOperationException> { bare.getLatestUpdates(1) }
        shouldThrow<UnsupportedOperationException> { bare.getSearchManga(1, "q", FilterList()) }
        shouldThrow<UnsupportedOperationException> { bare.getPageList(stored.single()) }
    }

    @Test
    fun mangaUpdateDefaultsThrow() = runTest {
        shouldThrow<UnsupportedOperationException> {
            bare.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = true, fetchChapters = false)
        }
        shouldThrow<UnsupportedOperationException> {
            bare.getMangaUpdate(manga = manga, chapters = stored, fetchDetails = false, fetchChapters = true)
        }
        bare.lang shouldBe "en"
    }
}
