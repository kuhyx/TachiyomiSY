package eu.kanade.tachiyomi.ui.deeplink

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ResolvableSource
import eu.kanade.tachiyomi.source.online.UriType
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChapterByUrlAndMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal class DeepLinkScreenModelTest {
    private val sourceManager = mockk<SourceManager>()
    private val networkToLocalManga = mockk<NetworkToLocalManga>()
    private val getChapter = mockk<GetChapterByUrlAndMangaId>()
    private val update = mockk<UpdateMangaFromRemote>()
    private val source = mockk<ResolvableSource> { every { id } returns 7L }
    private val manga = Manga.create().copy(id = 3L, url = "/m")
    private val sChapter = SChapter.create().also { it.url = "/c" }

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        every { sourceManager.getAll() } returns listOf(mockk<Source>(), source)
        coEvery { networkToLocalManga(any<Manga>()) } returns manga
        coEvery { source.getManga(QUERY) } returns SManga.create().also { it.url = "/m" }
        coEvery { source.getChapter(QUERY) } returns sChapter
    }

    @AfterEach
    fun tearDown() = mainReset()

    private fun result(): DeepLinkScreenModel.State = DeepLinkScreenModel(
        query = QUERY,
        sourceManager = sourceManager,
        networkToLocalManga = networkToLocalManga,
        getChapterByUrlAndMangaId = getChapter,
        updateMangaFromRemote = update,
    ).state.await { it != DeepLinkScreenModel.State.Loading }

    private fun remote(result: Result<RemoteMangaUpdate>) {
        coEvery { getChapter.await("/c", 3L) } returns null
        coEvery {
            update(
                manga = manga,
                fetchDetails = any(),
                fetchChapters = true,
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns result
    }

    @Test
    fun unknownUriHasNoResults() {
        every { source.getUriType(QUERY) } returns UriType.Unknown
        result() shouldBe DeepLinkScreenModel.State.NoResults
    }

    @Test
    fun unresolvedMangaHasNoResults() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        coEvery { source.getManga(QUERY) } returns null
        result() shouldBe DeepLinkScreenModel.State.NoResults
    }

    @Test
    fun mangaUriOpensTheManga() {
        every { source.getUriType(QUERY) } returns UriType.Manga
        result() shouldBe DeepLinkScreenModel.State.Result(manga)
    }

    @Test
    fun unresolvedChapterOpensManga() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        coEvery { source.getChapter(QUERY) } returns null
        result() shouldBe DeepLinkScreenModel.State.Result(manga)
    }

    @Test
    fun localChapterIsOpened() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        coEvery { getChapter.await("/c", 3L) } returns Chapter.create().copy(id = 9L)
        result() shouldBe DeepLinkScreenModel.State.Result(manga, chapterId = 9L)
    }

    @Test
    fun remoteChapterIsOpened() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        val chapters = listOf(Chapter.create().copy(id = 1L, url = "/x"), Chapter.create().copy(id = 2L, url = "/c"))
        remote(Result.success(RemoteMangaUpdate(manga, chapters)))
        result() shouldBe DeepLinkScreenModel.State.Result(manga, chapterId = 2L)
    }

    @Test
    fun missingRemoteChapterOpensManga() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        remote(Result.success(RemoteMangaUpdate(manga, emptyList())))
        result() shouldBe DeepLinkScreenModel.State.Result(manga)
    }

    @Test
    fun failedUpdateOpensManga() {
        every { source.getUriType(QUERY) } returns UriType.Chapter
        remote(Result.failure(IllegalStateException("offline")))
        result() shouldBe DeepLinkScreenModel.State.Result(manga)
    }

    @Test
    fun noResolvableSourceHasNoResults() {
        every { sourceManager.getAll() } returns emptyList()
        result() shouldBe DeepLinkScreenModel.State.NoResults
    }

    @Test
    fun emptyQueryResolvesNothing() {
        every { source.getUriType("") } returns UriType.Unknown
        val model = DeepLinkScreenModel(
            sourceManager = sourceManager,
            networkToLocalManga = networkToLocalManga,
            getChapterByUrlAndMangaId = getChapter,
            updateMangaFromRemote = update,
        )
        model.state.await { it != DeepLinkScreenModel.State.Loading } shouldBe DeepLinkScreenModel.State.NoResults
    }

    @Test
    fun stateMembers() {
        val state = DeepLinkScreenModel.State.Result(manga)
        state.copy(chapterId = 1L).chapterId shouldBe 1L
        state.hashCode() shouldBe DeepLinkScreenModel.State.Result(manga).hashCode()
        DeepLinkScreenModel.State.Loading.toString() shouldBe "Loading"
        DeepLinkScreenModel.State.NoResults.toString() shouldBe "NoResults"
    }
}

private const val QUERY = "https://example.org/q"
