package tachiyomi.domain.manga.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

internal class SetMangaChapterFlagsTest {

    private val repository = mockk<MangaRepository>()
    private val setFlags = SetMangaChapterFlags(repository)
    private val update = slot<MangaUpdate>()

    // Every filter and mode bit set, so each setter must clear only its own mask.
    private val allBits = Manga.CHAPTER_SHOW_READ or Manga.CHAPTER_SHOW_NOT_DOWNLOADED or
        Manga.CHAPTER_SHOW_NOT_BOOKMARKED or Manga.CHAPTER_SORTING_ALPHABET or Manga.CHAPTER_DISPLAY_NUMBER
    private val manga = MangaFixtures.manga(id = 6L).copy(chapterFlags = allBits)

    @BeforeEach
    fun stubUpdate() {
        coEvery { repository.update(capture(update)) } returns true
    }

    @Test
    fun downloadedFilterReplacesMask() = runTest {
        setFlags.awaitSetDownloadedFilter(manga, Manga.CHAPTER_SHOW_DOWNLOADED) shouldBe true

        val expected = allBits and Manga.CHAPTER_DOWNLOADED_MASK.inv() or Manga.CHAPTER_SHOW_DOWNLOADED
        update.captured.id shouldBe 6L
        update.captured.chapterFlags shouldBe expected
    }

    @Test
    fun unreadFilterReplacesMask() = runTest {
        setFlags.awaitSetUnreadFilter(manga, Manga.CHAPTER_SHOW_UNREAD) shouldBe true

        update.captured.chapterFlags shouldBe (allBits and Manga.CHAPTER_UNREAD_MASK.inv() or Manga.CHAPTER_SHOW_UNREAD)
    }

    @Test
    fun bookmarkFilterReplacesMask() = runTest {
        setFlags.awaitSetBookmarkFilter(manga, Manga.SHOW_ALL) shouldBe true

        update.captured.chapterFlags shouldBe (allBits and Manga.CHAPTER_BOOKMARKED_MASK.inv())
    }

    @Test
    fun displayModeReplacesMask() = runTest {
        setFlags.awaitSetDisplayMode(manga, Manga.CHAPTER_DISPLAY_NAME) shouldBe true

        update.captured.chapterFlags shouldBe (allBits and Manga.CHAPTER_DISPLAY_MASK.inv())
    }

    @Test
    fun sameSortFlipsDescToAsc() = runTest {
        setFlags.awaitSetSortingModeOrFlipOrder(manga, Manga.CHAPTER_SORTING_ALPHABET) shouldBe true

        update.captured.chapterFlags shouldBe (allBits or Manga.CHAPTER_SORT_ASC)
    }

    @Test
    fun sameSortFlipsAscToDesc() = runTest {
        val ascending = manga.copy(chapterFlags = allBits or Manga.CHAPTER_SORT_ASC)

        setFlags.awaitSetSortingModeOrFlipOrder(ascending, Manga.CHAPTER_SORTING_ALPHABET) shouldBe true

        update.captured.chapterFlags shouldBe allBits
    }

    @Test
    fun newSortStartsAscending() = runTest {
        setFlags.awaitSetSortingModeOrFlipOrder(manga, Manga.CHAPTER_SORTING_NUMBER) shouldBe true

        val expected = allBits and Manga.CHAPTER_SORTING_MASK.inv() or Manga.CHAPTER_SORTING_NUMBER or
            Manga.CHAPTER_SORT_ASC
        update.captured.chapterFlags shouldBe expected
    }

    @Test
    fun allFlagsAreCombined() = runTest {
        val isUpdated = setFlags.awaitSetAllFlags(
            mangaId = 6L,
            unreadFilter = Manga.CHAPTER_SHOW_UNREAD,
            downloadedFilter = Manga.CHAPTER_SHOW_NOT_DOWNLOADED,
            bookmarkedFilter = Manga.CHAPTER_SHOW_BOOKMARKED,
            sortingMode = Manga.CHAPTER_SORTING_UPLOAD_DATE,
            sortingDirection = Manga.CHAPTER_SORT_ASC,
            displayMode = Manga.CHAPTER_DISPLAY_NUMBER,
        )

        val expected = Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_NOT_DOWNLOADED or
            Manga.CHAPTER_SHOW_BOOKMARKED or Manga.CHAPTER_SORTING_UPLOAD_DATE or Manga.CHAPTER_SORT_ASC or
            Manga.CHAPTER_DISPLAY_NUMBER
        isUpdated shouldBe true
        update.captured.id shouldBe 6L
        update.captured.chapterFlags shouldBe expected
    }

    @Test
    fun flagOutsideMaskIsIgnored() = runTest {
        setFlags.awaitSetUnreadFilter(manga, Manga.CHAPTER_DISPLAY_NUMBER) shouldBe true

        update.captured.chapterFlags shouldBe (allBits and Manga.CHAPTER_UNREAD_MASK.inv())
    }

    @Test
    fun repositoryResultIsReturned() = runTest {
        coEvery { repository.update(any()) } returns false

        setFlags.awaitSetDisplayMode(manga, Manga.CHAPTER_DISPLAY_NUMBER) shouldBe false
    }
}
