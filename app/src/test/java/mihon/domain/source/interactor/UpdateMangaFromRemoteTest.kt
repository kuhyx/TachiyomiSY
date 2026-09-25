package mihon.domain.source.interactor

import eu.kanade.tachiyomi.data.download.renameManga
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.source.online.all.getMangaUpdate
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import mihon.domain.source.interactor.UpdateMangaFromRemoteHarness.Companion.remoteChapters
import mihon.domain.source.interactor.UpdateMangaFromRemoteHarness.Companion.updatedManga
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.source.local.LocalSource
import kotlin.io.path.createTempFile

internal class UpdateMangaFromRemoteTest {

    private val harness = UpdateMangaFromRemoteHarness()
    private val interactor = harness.interactor

    @BeforeEach
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerRenamesKt")
        coEvery { harness.downloadManager.renameManga(any(), any()) } returns Unit
        val customMangaRepository = mockk<CustomMangaRepository>()
        every { customMangaRepository.get(any()) } returns null
        every { customMangaRepository.set(any<CustomMangaInfo>()) } returns Unit
        startKoin { modules(module { single { GetCustomMangaInfo(customMangaRepository) } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun resolvesSourceFromManager() = runTest {
        val source = harness.plainSource(remoteManga())
        every { harness.sourceManager.getOrStub(7) } returns source
        val result = interactor(localManga())
        result.getOrThrow() shouldBe RemoteMangaUpdate(updatedManga, listOf(Chapter.create().copy(id = 9)))
        coVerify(exactly = 1) { source.getMangaUpdate(any(), emptyList(), false, false) }
        coVerify(exactly = 1) {
            harness.syncChaptersWithSource.await(remoteChapters, any(), source, false, any())
        }
    }

    @Test
    fun updatesTitleAndCoverWhenNew() = runTest {
        val stored = listOf(Chapter.create().copy(id = 1, sourceOrder = 1))
        val source = harness.plainSource(remoteManga(), stored = stored)
        val result = interactor(source, localManga(), fetchDetails = true, fetchChapters = true, manualFetch = true)
        result.isSuccess shouldBe true
        val update = harness.mangaUpdates.single()
        update.title shouldBe "Remote"
        update.thumbnailUrl shouldBe "http://cover"
        (update.coverLastModified!! > 0L) shouldBe true
        update.author shouldBe "A"
        update.initialized shouldBe true
        coVerify(exactly = 1) { harness.downloadManager.renameManga(any(), "Remote") }
        coVerify(exactly = 1) { harness.coverCache.deleteFromCache(any(), false) }
        coVerify(exactly = 1) { source.getMangaUpdate(any(), any(), true, true) }
    }

    @Test
    fun keepsFavouriteTitles() = runTest {
        val source = harness.plainSource(remoteManga(thumbnail = "http://old"))
        interactor(source, localManga(favorite = true)).isSuccess shouldBe true
        harness.mangaUpdates.last().title shouldBe null
        harness.mangaUpdates.last().coverLastModified shouldBe null
        harness.libraryPreferences.updateMangaTitles.set(true)
        interactor(source, localManga(favorite = true)).isSuccess shouldBe true
        harness.mangaUpdates.last().title shouldBe "Remote"
        coVerify(exactly = 1) { harness.downloadManager.renameManga(any(), "Remote") }
    }

    @Test
    fun uninitialisedTitleIsIgnored() = runTest {
        val remote = mockk<SManga>(relaxed = true)
        every { remote.title } throws UninitializedPropertyAccessException("title")
        every { remote.thumbnail_url } returns null
        every { remote.getGenres() } returns null
        every { remote.memo } returns JsonObject(emptyMap())
        val source = harness.plainSource(remote)
        interactor(source, localManga()).isSuccess shouldBe true
        harness.mangaUpdates.single().title shouldBe null
        harness.mangaUpdates.single().thumbnailUrl shouldBe null
        harness.mangaUpdates.single().coverLastModified shouldBe null
        coVerify(exactly = 0) { harness.downloadManager.renameManga(any(), any()) }
    }

    @Test
    fun coverHandlingPerCase() = runTest {
        val source = harness.plainSource(remoteManga(thumbnail = ""))
        interactor(source, localManga()).isSuccess shouldBe true
        harness.mangaUpdates.last().coverLastModified shouldBe null
        harness.mangaUpdates.last().thumbnailUrl shouldBe null
        val local = harness.plainSource(remoteManga())
        interactor(local, localManga(source = LocalSource.ID)).isSuccess shouldBe true
        (harness.mangaUpdates.last().coverLastModified!! > 0L) shouldBe true
        coVerify(exactly = 0) { harness.coverCache.deleteFromCache(any(), any()) }
        every { harness.coverCache.getCustomCoverFile(4) } returns createTempFile("cover").toFile()
        interactor(local, localManga()).isSuccess shouldBe true
        harness.mangaUpdates.last().coverLastModified shouldBe null
        coVerify(exactly = 1) { harness.coverCache.deleteFromCache(any(), false) }
    }

    @Test
    fun aRejectedUpdateSkipsTheRename() = runTest {
        val source = harness.plainSource(remoteManga())
        coEvery { harness.mangaRepository.update(any()) } returns false
        interactor(source, localManga()).isSuccess shouldBe true
        coVerify(exactly = 0) { harness.downloadManager.renameManga(any(), any()) }
    }

    @Test
    fun failuresAreReturnedNotThrown() = runTest {
        val source = harness.plainSource(remoteManga())
        val failure = IllegalStateException("offline")
        coEvery { source.getMangaUpdate(any(), any(), any(), any()) } throws failure
        interactor(source, localManga()).exceptionOrNull() shouldBe failure
    }

    @Test
    fun ehentaiGetsTheThrottle() = runTest {
        mockkStatic("eu.kanade.tachiyomi.source.online.all.EHentaiDetailsKt")
        val plain = harness.plainSource(remoteManga(), stored = listOf(Chapter.create().copy(id = 1)))
        val eh = mockk<EHentai> { every { id } returns 7L }
        var throttled = 0
        coEvery { eh.getMangaUpdate(any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend () -> Unit>(5).invoke()
            throttled++
            plain.getMangaUpdate(arg(1), arg(2), arg(3), arg(4))
        }
        every { harness.sourceManager.getOrStub(7) } returns eh
        coEvery { harness.syncChaptersWithSource.await(remoteChapters, any(), eh, any(), any()) } returns emptyList()
        interactor(eh, localManga()).getOrThrow().newChapters shouldBe emptyList()
        interactor(localManga()).getOrThrow().newChapters shouldBe emptyList()
        interactor(
            manga = localManga(),
            fetchDetails = true,
            fetchChapters = true,
            manualFetch = true,
            fetchWindow = 1L to 2L,
        ) { }
        throttled shouldBe 3
        coVerify(exactly = 2) { eh.getMangaUpdate(any(), any(), false, false, any()) }
        coVerify(exactly = 1) { eh.getMangaUpdate(any(), any(), true, true, any()) }
    }

    @Test
    fun mergedSourcesSyncThemselves() = runTest {
        val plain = harness.plainSource(remoteManga())
        val merged = mockk<MergedSource> { every { id } returns 7L }
        coEvery { merged.getMangaUpdate(any(), any(), any(), any()) } coAnswers {
            plain.getMangaUpdate(firstArg(), secondArg(), thirdArg(), arg(3))
        }
        coEvery { merged.fetchChaptersAndSync(any(), downloadChapters = true) } returns listOf(Chapter.create())
        coEvery { merged.fetchChaptersAndSync(any(), downloadChapters = false) } returns emptyList()
        interactor(merged, localManga(), manualFetch = true).getOrThrow().newChapters.size shouldBe 1
        interactor(merged, localManga()).getOrThrow().newChapters.size shouldBe 0
        coVerify(exactly = 0) { harness.syncChaptersWithSource.await(any(), any(), any(), any(), any()) }
    }
}
