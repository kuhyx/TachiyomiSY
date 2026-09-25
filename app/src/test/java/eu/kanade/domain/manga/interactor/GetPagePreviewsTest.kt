package eu.kanade.domain.manga.interactor

import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class GetPagePreviewsTest {

    private val cache = mockk<PagePreviewCache>()
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val interactor = GetPagePreviews(cache, getChaptersByMangaId)
    private val manga = Manga.create().copy(id = 4)
    private val source = mockk<PagePreviewSource>(moreInterfaces = arrayOf(Source::class)) { every { id } returns 7L }
    private val chapters = listOf(
        Chapter.create().copy(id = 1, sourceOrder = 1),
        Chapter.create().copy(id = 2, sourceOrder = 0),
    )
    private val page = PagePreviewPage(
        page = 1,
        pagePreviews = listOf(PagePreviewInfo(index = 0, imageUrl = "http://a")),
        hasNextPage = true,
        pagePreviewPages = 3,
    )

    @Test
    fun plainSourcesAreUnused() = runTest {
        interactor.await(manga, mockk<Source>(), 1) shouldBe GetPagePreviews.Result.Unused
    }

    @Test
    fun servesFromTheCache() = runTest {
        coEvery { getChaptersByMangaId.await(4) } returns chapters
        every { cache.getPageListFromCache(manga, listOf(1, 2), 1) } returns page
        val result = interactor.await(manga, source as Source, 1)
        result shouldBe GetPagePreviews.Result.Success(
            pagePreviews = listOf(PagePreview(index = 0, imageUrl = "http://a", source = 7)),
            hasNextPage = true,
            pageCount = 3,
        )
        coVerify(exactly = 0) { source.getPagePreviewList(any(), any(), any()) }
    }

    @Test
    fun fetchesAndCachesOnAMiss() = runTest {
        coEvery { getChaptersByMangaId.await(4) } returns chapters
        every { cache.getPageListFromCache(manga, listOf(1, 2), 1) } throws IllegalStateException("miss")
        coEvery { source.getPagePreviewList(any(), any(), 1) } returns page
        every { cache.putPageListToCache(manga, listOf(1, 2), page) } returns Unit
        val result = interactor.await(manga, source as Source, 1)
        result.shouldBeInstanceOf<GetPagePreviews.Result.Success>().pagePreviews.single().source shouldBe 7L
        verify(exactly = 1) { cache.putPageListToCache(manga, listOf(1, 2), page) }
        val lastPage = page.copy(hasNextPage = false, pagePreviewPages = null)
        every { cache.getPageListFromCache(manga, listOf(1, 2), 2) } returns lastPage
        val end = interactor.await(manga, source as Source, 2)
        end.shouldBeInstanceOf<GetPagePreviews.Result.Success>().pageCount shouldBe null
    }

    @Test
    fun reportsFailures() = runTest {
        val failure = IllegalStateException("offline")
        coEvery { getChaptersByMangaId.await(4) } returns chapters
        every { cache.getPageListFromCache(manga, listOf(1, 2), 1) } throws IllegalStateException("miss")
        coEvery { source.getPagePreviewList(any(), any(), 1) } throws failure
        interactor.await(manga, source as Source, 1) shouldBe GetPagePreviews.Result.Error(failure)
    }
}
