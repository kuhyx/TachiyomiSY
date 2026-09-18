package tachiyomi.domain.manga.model

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import java.time.Instant

internal class MangaChapterFlagsTest {

    private val manga = MangaFixtures.manga()

    @Test
    fun nextUpdateWhenOngoing() {
        val ongoing = manga.copy(nextUpdate = 1_000L, ogStatus = SManga.ONGOING.toLong())

        ongoing.expectedNextUpdate shouldBe Instant.ofEpochMilli(1_000L)
    }

    @Test
    fun nextUpdateNullWhenCompleted() {
        val completed = manga.copy(nextUpdate = 1_000L, ogStatus = SManga.COMPLETED.toLong())

        completed.expectedNextUpdate shouldBe null
    }

    @Test
    fun sortingMasksTheSortBits() {
        val flags = Manga.CHAPTER_SORTING_UPLOAD_DATE or Manga.CHAPTER_DISPLAY_NUMBER or Manga.CHAPTER_SHOW_READ

        manga.copy(chapterFlags = flags).sorting shouldBe Manga.CHAPTER_SORTING_UPLOAD_DATE
        manga.copy(chapterFlags = Manga.CHAPTER_SORTING_ALPHABET).sorting shouldBe Manga.CHAPTER_SORTING_ALPHABET
        manga.sorting shouldBe Manga.CHAPTER_SORTING_SOURCE
    }

    @Test
    fun displayModeMasksTheBit() {
        val flags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_DISPLAY_NUMBER

        manga.copy(chapterFlags = flags).displayMode shouldBe Manga.CHAPTER_DISPLAY_NUMBER
        manga.displayMode shouldBe Manga.CHAPTER_DISPLAY_NAME
    }

    @Test
    fun rawFiltersMaskTheirBits() {
        val flags = Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_DOWNLOADED or Manga.CHAPTER_SHOW_NOT_BOOKMARKED
        val filtered = manga.copy(chapterFlags = flags)

        filtered.unreadFilterRaw shouldBe Manga.CHAPTER_SHOW_UNREAD
        filtered.downloadedFilterRaw shouldBe Manga.CHAPTER_SHOW_DOWNLOADED
        filtered.bookmarkedFilterRaw shouldBe Manga.CHAPTER_SHOW_NOT_BOOKMARKED
        manga.unreadFilterRaw shouldBe Manga.SHOW_ALL
        manga.downloadedFilterRaw shouldBe Manga.SHOW_ALL
        manga.bookmarkedFilterRaw shouldBe Manga.SHOW_ALL
    }

    @Test
    fun unreadFilterTriState() {
        manga.copy(chapterFlags = Manga.CHAPTER_SHOW_UNREAD).unreadFilter shouldBe TriState.ENABLED_IS
        manga.copy(chapterFlags = Manga.CHAPTER_SHOW_READ).unreadFilter shouldBe TriState.ENABLED_NOT
        manga.unreadFilter shouldBe TriState.DISABLED
        manga.copy(chapterFlags = Manga.CHAPTER_UNREAD_MASK).unreadFilter shouldBe TriState.DISABLED
    }

    @Test
    fun bookmarkedFilterTriState() {
        manga.copy(chapterFlags = Manga.CHAPTER_SHOW_BOOKMARKED).bookmarkedFilter shouldBe TriState.ENABLED_IS
        manga.copy(chapterFlags = Manga.CHAPTER_SHOW_NOT_BOOKMARKED).bookmarkedFilter shouldBe TriState.ENABLED_NOT
        manga.bookmarkedFilter shouldBe TriState.DISABLED
        manga.copy(chapterFlags = Manga.CHAPTER_BOOKMARKED_MASK).bookmarkedFilter shouldBe TriState.DISABLED
    }

    @Test
    fun sortDescendingReadsDirBit() {
        manga.copy(chapterFlags = Manga.CHAPTER_SORT_DESC).sortDescending() shouldBe true
        manga.copy(chapterFlags = Manga.CHAPTER_SORT_ASC).sortDescending() shouldBe false
        val ascByNumber = Manga.CHAPTER_SORT_ASC or Manga.CHAPTER_SORTING_NUMBER
        manga.copy(chapterFlags = ascByNumber).sortDescending() shouldBe false
    }

    @Test
    fun maskConstantsCoverTheirFlags() {
        Manga.CHAPTER_UNREAD_MASK shouldBe (Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_READ)
        Manga.CHAPTER_DOWNLOADED_MASK shouldBe (Manga.CHAPTER_SHOW_DOWNLOADED or Manga.CHAPTER_SHOW_NOT_DOWNLOADED)
        Manga.CHAPTER_BOOKMARKED_MASK shouldBe (Manga.CHAPTER_SHOW_BOOKMARKED or Manga.CHAPTER_SHOW_NOT_BOOKMARKED)
        Manga.CHAPTER_SORTING_MASK shouldBe Manga.CHAPTER_SORTING_ALPHABET
        Manga.CHAPTER_DISPLAY_MASK shouldBe Manga.CHAPTER_DISPLAY_NUMBER
        Manga.CHAPTER_SORT_DIR_MASK shouldBe Manga.CHAPTER_SORT_ASC
    }
}
