package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

private fun libraryManga(readCount: Long, bookmarkCount: Long): LibraryManga = LibraryManga(
    manga = Manga.create().copy(id = 42L),
    categories = listOf(1L, 2L),
    totalChapters = 10L,
    readCount = readCount,
    bookmarkCount = bookmarkCount,
    latestUpload = 300L,
    chapterFetchedAt = 200L,
    lastRead = 100L,
)

internal class LibraryMangaTest {

    @Test
    fun idComesFromManga() {
        libraryManga(readCount = 0L, bookmarkCount = 0L).id shouldBe 42L
    }

    @Test
    fun unreadCountIsRemainder() {
        libraryManga(readCount = 3L, bookmarkCount = 0L).unreadCount shouldBe 7L
        libraryManga(readCount = 10L, bookmarkCount = 0L).unreadCount shouldBe 0L
    }

    @Test
    fun hasBookmarksWhenBookmarked() {
        libraryManga(readCount = 0L, bookmarkCount = 1L).hasBookmarks shouldBe true
        libraryManga(readCount = 0L, bookmarkCount = 0L).hasBookmarks shouldBe false
    }

    @Test
    fun hasStartedWhenAnyRead() {
        libraryManga(readCount = 1L, bookmarkCount = 0L).hasStarted shouldBe true
        libraryManga(readCount = 0L, bookmarkCount = 0L).hasStarted shouldBe false
    }

    @Test
    fun isADataClass() {
        val entry = libraryManga(readCount = 2L, bookmarkCount = 1L)
        val copy = entry.copy(readCount = 3L)
        copy shouldNotBe entry
        copy.copy(readCount = 2L) shouldBe entry
        copy.hashCode() shouldBe entry.copy(readCount = 3L).hashCode()
        entry.toString().startsWith("LibraryManga(manga=") shouldBe true
    }

    @Test
    fun exposesEveryComponent() {
        val entry = libraryManga(readCount = 2L, bookmarkCount = 1L)
        val (manga, categories, totalChapters) = entry
        manga.id shouldBe 42L
        categories shouldBe listOf(1L, 2L)
        totalChapters shouldBe 10L
        entry.component4() shouldBe 2L
        entry.component5() shouldBe 1L
        entry.component6() shouldBe 300L
        entry.component7() shouldBe 200L
        entry.component8() shouldBe 100L
    }
}
