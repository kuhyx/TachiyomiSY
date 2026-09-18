package mihon.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.chapterOf
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga

/** The "wanted at all" gate and the read-number filter; categories are in [FilterChaptersForDownloadCategoryTest]. */
internal class FilterChaptersForDownloadTest {

    private val getChapters = mockk<GetChaptersByMangaId>()
    private val getMergedChapters = mockk<GetMergedChaptersByMangaId>()
    private val getCategories = mockk<GetCategories>()
    private val preferences = DownloadPreferences(InMemoryPreferenceStore())
    private val filter = FilterChaptersForDownload(
        getChaptersByMangaId = getChapters,
        getMergedChaptersByMangaId = getMergedChapters,
        downloadPreferences = preferences,
        getCategories = getCategories,
    )
    private val newChapters = listOf(chapterOf(1L, 1L, 1.0), chapterOf(2L, 1L, 2.0), chapterOf(3L, 1L, -1.0))

    // Already in the store: chapter 1 was read, an unnumbered one was read, chapter 2 was not.
    private val existing = listOf(
        chapterOf(4L, 1L, 1.0).copy(read = true),
        chapterOf(5L, 1L, -1.0).copy(read = true),
        chapterOf(6L, 1L, 2.0).copy(read = false),
    )

    @BeforeEach
    fun setUp() {
        preferences.downloadNewChapters.set(true)
        coEvery { getCategories.await(1L) } returns emptyList()
    }

    @Test
    fun nothingNewDownloadsNothing() = runTest {
        filter.await(favoriteManga(id = 1L, source = 3L), emptyList()) shouldBe emptyList()
    }

    @Test
    fun downloadOffDownloadsNothing() = runTest {
        preferences.downloadNewChapters.set(false)

        filter.await(favoriteManga(id = 1L, source = 3L), newChapters) shouldBe emptyList()
    }

    @Test
    fun nonFavoriteDownloadsNothing() = runTest {
        filter.await(Manga.create().copy(id = 1L, source = 3L), newChapters) shouldBe emptyList()

        coVerify(exactly = 0) { getCategories.await(any()) }
    }

    @Test
    fun everyNewChapterByDefault() = runTest {
        filter.await(favoriteManga(id = 1L, source = 3L), newChapters) shouldBe newChapters
    }

    @Test
    fun unreadOnlySkipsReadNumbers() = runTest {
        preferences.downloadNewUnreadChaptersOnly.set(true)
        coEvery { getChapters.await(1L, false) } returns existing

        val result = filter.await(favoriteManga(id = 1L, source = 3L), newChapters)

        result shouldBe listOf(newChapters[1], newChapters[2])
    }

    @Test
    fun unreadOnlyOnMergedManga() = runTest {
        preferences.downloadNewUnreadChaptersOnly.set(true)
        coEvery { getMergedChapters.await(mangaId = 1L, dedupe = true, applyScanlatorFilter = false) } returns existing

        val result = filter.await(favoriteManga(id = 1L, source = MERGED_SOURCE_ID), newChapters)

        result shouldBe listOf(newChapters[1], newChapters[2])
    }
}
