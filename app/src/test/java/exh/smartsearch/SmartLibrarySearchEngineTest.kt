package exh.smartsearch

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

internal class SmartLibrarySearchEngineTest {
    private val library by lazy {
        listOf(libraryManga("Made in Abyss"), libraryManga("Some Other Series"))
    }

    private fun libraryManga(title: String) = LibraryManga(
        manga = Manga.create().copy(ogTitle = title),
        categories = emptyList(),
        totalChapters = 0,
        readCount = 0,
        bookmarkCount = 0,
        latestUpload = 0,
        chapterFetchedAt = 0,
        lastRead = 0,
    )

    @Test
    fun findsTheCloseEnoughTitle() = runTest {
        val engine = SmartLibrarySearchEngine()
        engine.smartSearch(library, "Made in Abyss (Official)")?.manga?.ogTitle shouldBe "Made in Abyss"
    }

    @Test
    fun rejectsUnrelatedTitles() = runTest {
        SmartLibrarySearchEngine(extraSearchParams = "lang:en").smartSearch(library, "Nothing Alike").shouldBeNull()
    }
}
