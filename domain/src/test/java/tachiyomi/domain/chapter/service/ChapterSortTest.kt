package tachiyomi.domain.chapter.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeNegative
import io.kotest.matchers.ints.shouldBePositive
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.sorting

internal class ChapterSortTest {

    // `first` is earlier on every axis: source order, number, upload date and name.
    private val first = Chapter.create().copy(sourceOrder = 1L, chapterNumber = 1.0, dateUpload = 100L, name = "Alpha")
    private val second = Chapter.create().copy(sourceOrder = 2L, chapterNumber = 2.0, dateUpload = 200L, name = "Beta")

    private fun manga(sorting: Long, ascending: Boolean = false): Manga {
        val direction = if (ascending) Manga.CHAPTER_SORT_ASC else Manga.CHAPTER_SORT_DESC
        return Manga.create().copy(chapterFlags = sorting or direction)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("tachiyomi.domain.manga.model.MangaChapterFlagsKt")
    }

    @Test
    fun sourceOrderFollowsDirection() {
        // Descending source order lists the source's first chapter first.
        getChapterSort(manga(Manga.CHAPTER_SORTING_SOURCE), sortDescending = true)(first, second).shouldBeNegative()
        getChapterSort(manga(Manga.CHAPTER_SORTING_SOURCE), sortDescending = false)(first, second).shouldBePositive()
    }

    @Test
    fun numberFollowsDirection() {
        getChapterSort(manga(Manga.CHAPTER_SORTING_NUMBER), sortDescending = true)(first, second).shouldBePositive()
        getChapterSort(manga(Manga.CHAPTER_SORTING_NUMBER), sortDescending = false)(first, second).shouldBeNegative()
    }

    @Test
    fun uploadDateFollowsDirection() {
        val sorting = Manga.CHAPTER_SORTING_UPLOAD_DATE
        getChapterSort(manga(sorting), sortDescending = true)(first, second).shouldBePositive()
        getChapterSort(manga(sorting), sortDescending = false)(first, second).shouldBeNegative()
    }

    @Test
    fun alphabetFollowsDirection() {
        getChapterSort(manga(Manga.CHAPTER_SORTING_ALPHABET), sortDescending = true)(first, second).shouldBePositive()
        getChapterSort(manga(Manga.CHAPTER_SORTING_ALPHABET), sortDescending = false)(first, second).shouldBeNegative()
        getChapterSort(manga(Manga.CHAPTER_SORTING_ALPHABET), sortDescending = false)(first, first) shouldBe 0
    }

    @Test
    fun directionDefaultsToTheFlags() {
        getChapterSort(manga(Manga.CHAPTER_SORTING_NUMBER))(first, second).shouldBePositive()
        getChapterSort(manga(Manga.CHAPTER_SORTING_NUMBER, ascending = true))(first, second).shouldBeNegative()
    }

    @Test
    fun unknownSortingFails() {
        // The mask only yields the four known modes, so an unknown one is forced through the accessor.
        val manga = manga(Manga.CHAPTER_SORTING_SOURCE)
        mockkStatic("tachiyomi.domain.manga.model.MangaChapterFlagsKt")
        every { manga.sorting } returns 0x400L

        val failure = shouldThrow<IllegalStateException> { getChapterSort(manga, sortDescending = true) }

        failure.message shouldContain "1024"
    }
}
