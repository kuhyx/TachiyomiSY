package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.InjektStub
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.sChapter
import eu.kanade.tachiyomi.source.online.sManga
import exh.source.MERGED_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager

internal fun reference(
    mangaId: Long? = 2L,
    mangaSourceId: Long = 7L,
    mangaUrl: String = "/part",
    isInfoManga: Boolean = false,
    getChapterUpdates: Boolean = true,
    downloadChapters: Boolean = false,
    mergeId: Long? = 1L,
): MergedMangaReference = MergedMangaReference(
    id = 10L,
    isInfoManga = isInfoManga,
    getChapterUpdates = getChapterUpdates,
    chapterSortMode = 0,
    chapterPriority = 0,
    downloadChapters = downloadChapters,
    mergeId = mergeId,
    mergeUrl = "/merged",
    mangaId = mangaId,
    mangaUrl = mangaUrl,
    mangaSourceId = mangaSourceId,
)

internal class MergedSourceTest {
    private val stub = InjektStub()
    private val getManga = mockk<GetManga>()
    private val getReferences = mockk<GetMergedReferencesById>()
    private val networkToLocal = mockk<NetworkToLocalManga>()
    private val updateFromRemote = mockk<UpdateMangaFromRemote>()
    private val sourceManager = mockk<SourceManager>()
    private val downloadManager = mockk<DownloadManager>()
    private val filterForDownload = mockk<FilterChaptersForDownload>()
    private val partSource = mockk<Source>()
    private val merged = Manga.create().copy(id = 1L, url = "/merged", source = MERGED_SOURCE_ID, ogTitle = "Merged")
    private val part = Manga.create().copy(id = 2L, url = "/part", source = 7L, ogTitle = "Part")
    private val chapter = Chapter.create().copy(id = 5L, mangaId = 2L, url = "/c1", name = "c1")
    private lateinit var source: MergedSource

    @BeforeEach
    fun setUp() {
        stub.install()
        stub.serve(getManga)
        stub.serve(getReferences)
        stub.serve(networkToLocal)
        stub.serve(updateFromRemote)
        stub.serve(sourceManager)
        stub.serve(downloadManager)
        stub.serve(filterForDownload)
        every { sourceManager.getOrStub(any()) } returns partSource
        coEvery { getManga.await("/merged", MERGED_SOURCE_ID) } returns merged
        coEvery { getManga.await("/part", 7L) } returns part
        source = MergedSource()
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    // The interactor's trailing defaults (a fresh lambda each call) never match by equality, so they are wildcards.
    private fun remoteUpdate(manga: Manga, fetchDetails: Boolean) = coEvery {
        updateFromRemote(
            source = partSource,
            manga = manga,
            fetchDetails = fetchDetails,
            fetchChapters = !fetchDetails,
            manualFetch = false,
            fetchWindow = any(),
            throttleFunc = any(),
        )
    }

    @Test
    fun identityAndUnsupported() {
        source.id shouldBe MERGED_SOURCE_ID
        source.baseUrl shouldBe ""
        source.lang shouldBe "all"
        source.supportsLatest shouldBe false
        source.name shouldBe "MergedSource"
        val page = Page(0, "u")
        shouldThrow<UnsupportedOperationException> { runBlocking { source.getImage(page, 0L) } }
        shouldThrow<UnsupportedOperationException> { runBlocking { source.getImageUrl(page) } }
        shouldThrow<UnsupportedOperationException> { runBlocking { source.getPageList(sChapter("/c")) } }
        shouldThrow<UnsupportedOperationException> { runBlocking { source.getLatestUpdates(1) } }
        shouldThrow<UnsupportedOperationException> { runBlocking { source.getPopularManga(1) } }
        val helpers =
            listOf("fetchChapterList", "fetchImageUrl", "fetchPageList", "fetchLatestUpdates", "fetchPopularManga")
        for (name in helpers) {
            val arg: Any = when (name) {
                "fetchChapterList" -> sManga("/m")
                "fetchImageUrl" -> page
                "fetchPageList" -> sChapter("/c")
                else -> 1
            }
            shouldThrow<UnsupportedOperationException> { source.invokeDeclared(MergedSource::class, name, listOf(arg)) }
        }
    }

    @Test
    fun detailsFromInfoReference() {
        coEvery { getReferences.await(1L) } returns listOf(reference(isInfoManga = true))
        val details = runBlocking { source.getMangaDetails(sManga("/merged")) }
        details.title shouldBe "Part"
        details.url shouldBe "/merged"
        val update = runBlocking { source.getMangaUpdate(sManga("/merged"), emptyList(), true, true) }
        update.manga.title shouldBe "Part"
        update.chapters.isEmpty() shouldBe true
    }

    @Test
    fun detailsFallBackToForeign() {
        coEvery { getReferences.await(1L) } returns listOf(reference(mangaId = 1L, mergeId = 1L), reference())
        runBlocking { source.getMangaDetails(sManga("/merged")) }.title shouldBe "Part"
        coEvery { getReferences.await(1L) } returns listOf(reference(mangaId = 1L, mergeId = 1L, mangaUrl = "/merged"))
        runBlocking { source.getMangaDetails(sManga("/merged")) }.title shouldBe "Merged"
        coEvery { getManga.await("/part", 7L) } returns null
        coEvery { getReferences.await(1L) } returns listOf(reference(isInfoManga = true))
        runBlocking { source.getMangaDetails(sManga("/merged")) }.title shouldBe "Merged"
    }

    @Test
    fun detailsRejectCorruptMerges() {
        coEvery { getManga.await("/missing", MERGED_SOURCE_ID) } returns null
        shouldThrow<IllegalArgumentException> { runBlocking { source.getMangaDetails(sManga("/missing")) } }
        coEvery { getReferences.await(1L) } returns emptyList()
        shouldThrow<IllegalArgumentException> { runBlocking { source.getMangaDetails(sManga("/merged")) } }
        coEvery { getReferences.await(1L) } returns listOf(reference(mangaSourceId = MERGED_SOURCE_ID))
        shouldThrow<IllegalArgumentException> { runBlocking { source.getMangaDetails(sManga("/merged")) } }
        val self = reference(mangaId = 1L, mangaSourceId = MERGED_SOURCE_ID, mangaUrl = "/merged")
        coEvery { getReferences.await(1L) } returns listOf(self, reference())
        runBlocking { source.getMangaDetails(sManga("/merged")) }.title shouldBe "Part"
    }

    @Test
    fun chaptersFetchedAndDownloaded() {
        coEvery { getReferences.await(1L) } returns
            listOf(reference(downloadChapters = true), reference(mangaSourceId = MERGED_SOURCE_ID))
        remoteUpdate(part, fetchDetails = false) returns Result.success(RemoteMangaUpdate(part, listOf(chapter)))
        coEvery { filterForDownload.await(merged, listOf(chapter)) } returns listOf(chapter)
        justRun { downloadManager.downloadChapters(part, listOf(chapter)) }
        runBlocking { source.fetchChaptersAndSync(merged) } shouldContainExactly listOf(chapter)
        verify(exactly = 1) { downloadManager.downloadChapters(part, listOf(chapter)) }
        coEvery { filterForDownload.await(merged, listOf(chapter)) } returns emptyList()
        runBlocking { source.fetchChaptersForMergedManga(merged) }
        runBlocking { source.fetchChaptersForMergedManga(merged, downloadChapters = false) }
        runBlocking { source.fetchChaptersAndSync(merged, downloadChapters = false) }.size shouldBe 1
        verify(exactly = 1) { downloadManager.downloadChapters(any(), any()) }
    }

    @Test
    fun chaptersSkipUnfollowed() {
        coEvery { getReferences.await(1L) } returns listOf(reference(getChapterUpdates = false), reference())
        remoteUpdate(part, fetchDetails = false) returns Result.success(RemoteMangaUpdate(part, emptyList()))
        runBlocking { source.fetchChaptersAndSync(merged) }.isEmpty() shouldBe true
        coEvery { getReferences.await(1L) } returns emptyList()
        shouldThrow<IllegalArgumentException> { runBlocking { source.fetchChaptersAndSync(merged) } }
    }

    @Test
    fun chaptersReportPartFailure() {
        coEvery { getReferences.await(1L) } returns listOf(reference())
        remoteUpdate(part, fetchDetails = false) returns Result.failure(IllegalStateException("part down"))
        shouldThrow<IllegalStateException> { runBlocking { source.fetchChaptersAndSync(merged) } }.message shouldBe
            "part down"
        remoteUpdate(part, fetchDetails = false) throws CancellationException("cancel")
        shouldThrow<CancellationException> { runBlocking { source.fetchChaptersAndSync(merged) } }
    }

    @Test
    fun loadCreatesMissingManga() {
        coEvery { getManga.await("/new", 7L) } returns null
        val created = Manga.create().copy(id = 9L, url = "/new", source = 7L)
        coEvery { networkToLocal(any<Manga>()) } returns created
        remoteUpdate(created, fetchDetails = true) returns
            Result.success(RemoteMangaUpdate(created.copy(ogTitle = "Fetched"), emptyList()))
        val reference = reference(mangaId = null, mangaUrl = "/new")
        val loaded = runBlocking { with(source) { reference.load() } }
        loaded.source shouldBe partSource
        loaded.manga.title shouldBe "Fetched"
        loaded.reference shouldBe reference
        loaded.copy(manga = part).manga shouldBe part
        val existing = runBlocking { with(source) { reference().load() } }
        existing.manga shouldBe part
    }
}
