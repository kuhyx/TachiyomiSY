package mihon.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.chapterOf
import tachiyomi.domain.download.service.DownloadPreferences

/** The category include/exclude gate of [FilterChaptersForDownload]. */
internal class FilterChaptersForDownloadCategoryTest {

    private val getCategories = mockk<GetCategories>()
    private val preferences = DownloadPreferences(InMemoryPreferenceStore())
    private val filter = FilterChaptersForDownload(
        getChaptersByMangaId = mockk(),
        getMergedChaptersByMangaId = mockk(),
        downloadPreferences = preferences,
        getCategories = getCategories,
    )
    private val newChapters = listOf(chapterOf(1L, 1L, 1.0))
    private val inCategory5 = listOf(Category(id = 5L, name = "Reading", order = 0L, flags = 0L))

    @BeforeEach
    fun setUp() {
        preferences.downloadNewChapters.set(true)
        coEvery { getCategories.await(1L) } returns inCategory5
    }

    private suspend fun download(): List<Chapter> =
        filter.await(favoriteManga(id = 1L, source = 3L), newChapters)

    @Test
    fun noCategoryRulesDownloadsAll() = runTest {
        download() shouldBe newChapters
    }

    @Test
    fun excludedCategoryBlocks() = runTest {
        preferences.downloadNewChapterCategoriesExclude.set(setOf("5"))

        download() shouldBe emptyList()
    }

    @Test
    fun otherCategoryExcluded() = runTest {
        preferences.downloadNewChapterCategoriesExclude.set(setOf("9"))

        download() shouldBe newChapters
    }

    @Test
    fun includedCategoryAllows() = runTest {
        preferences.downloadNewChapterCategories.set(setOf("5"))

        download() shouldBe newChapters
    }

    @Test
    fun otherCategoryIncluded() = runTest {
        preferences.downloadNewChapterCategories.set(setOf("9"))

        download() shouldBe emptyList()
    }

    @Test
    fun excludedBeatsIncluded() = runTest {
        preferences.downloadNewChapterCategories.set(setOf("5"))
        preferences.downloadNewChapterCategoriesExclude.set(setOf("5"))

        download() shouldBe emptyList()
    }

    @Test
    fun uncategorisedUsesDefault() = runTest {
        coEvery { getCategories.await(1L) } returns emptyList()

        preferences.downloadNewChapterCategories.set(setOf("0"))
        download() shouldBe newChapters

        preferences.downloadNewChapterCategoriesExclude.set(setOf("0"))
        download() shouldBe emptyList()
    }
}
