package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

internal class ChapterListTest {
    private val unread = item(chapter(1L))
    private val read = item(chapter(2L, read = true))
    private val bookmarked = item(chapter(3L, bookmark = true), state = Download.State.DOWNLOADED)
    private val rows = listOf(unread, read, bookmarked)

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { BasePreferences(mockk(relaxed = true), FlowPreferenceStore()) } }) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun List<ChapterList.Item>.ids(manga: Manga) = applyFilters(manga).map { it.id }.toList()

    @Test
    fun noFlagsSortsBySourceDesc() {
        rows.ids(manga()) shouldContainExactly listOf(3L, 2L, 1L)
    }

    @Test
    fun readFiltersApply() {
        rows.ids(manga(Manga.CHAPTER_SHOW_UNREAD)) shouldContainExactly listOf(3L, 1L)
        rows.ids(manga(Manga.CHAPTER_SHOW_READ)) shouldContainExactly listOf(2L)
    }

    @Test
    fun bookmarkFiltersApply() {
        rows.ids(manga(Manga.CHAPTER_SHOW_BOOKMARKED)) shouldContainExactly listOf(3L)
        rows.ids(manga(Manga.CHAPTER_SHOW_NOT_BOOKMARKED)) shouldContainExactly listOf(2L, 1L)
    }

    @Test
    fun downloadFilterApplies() {
        rows.ids(manga(Manga.CHAPTER_SHOW_DOWNLOADED)) shouldContainExactly listOf(3L)
        rows.ids(manga(Manga.CHAPTER_SHOW_NOT_DOWNLOADED)) shouldContainExactly listOf(2L, 1L)
    }

    @Test
    fun localMangaCountsAsDownloaded() {
        val local = manga(Manga.CHAPTER_SHOW_DOWNLOADED, source = LocalSource.ID)
        rows.ids(local) shouldContainExactly listOf(3L, 2L, 1L)
    }

    @Test
    fun ascendingGapsAreCounted() {
        val list = listOf(item(chapter(3L)), item(chapter(5L)))
        list.insertMissingCounts(manga(Manga.CHAPTER_SORT_ASC)) shouldContainExactly listOf(
            ChapterList.MissingCount(id = "null-3", count = 2),
            list[0],
            ChapterList.MissingCount(id = "3-5", count = 1),
            list[1],
        )
    }

    @Test
    fun descendingGapsAreCounted() {
        val list = listOf(item(chapter(5L)), item(chapter(3L)))
        list.insertMissingCounts(manga()) shouldContainExactly listOf(
            list[0],
            ChapterList.MissingCount(id = "3-5", count = 1),
            list[1],
            ChapterList.MissingCount(id = "null-3", count = 2),
        )
    }

    @Test
    fun noGapNoSeparator() {
        val list = listOf(item(chapter(1L)), item(chapter(2L)))
        list.insertMissingCounts(manga(Manga.CHAPTER_SORT_ASC)) shouldContainExactly list
    }

    @Test
    fun modelsBehaveAsValues() {
        unread.isDownloaded shouldBe false
        bookmarked.isDownloaded shouldBe true
        unread.copy(selected = true).selected shouldBe true
        ChapterList.MissingCount("a", 1).copy(count = 2).count shouldBe 2
        val data = MergedMangaData(emptyList(), emptyMap(), emptyList())
        data.copy() shouldBe data
        val preview = PagePreviewState.Success(listOf(PagePreview(index = 1, imageUrl = "u", source = 7L)))
        preview.copy().pagePreviews.size shouldBe 1
        val error = IllegalStateException("x")
        PagePreviewState.Error(error).copy().error shouldBe error
        PagePreviewState.Unused.toString() shouldBe "Unused"
        PagePreviewState.Loading.toString() shouldBe "Loading"
    }
}
