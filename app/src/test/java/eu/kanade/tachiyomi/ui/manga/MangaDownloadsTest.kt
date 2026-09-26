package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference

/** The file of the download manager's deletion extensions, spied on to see what the screen deleted. */
internal const val DELETION: String = "eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt"

/** A queued download of [chapter] in [state]. */
internal fun download(chapter: Chapter, state: Download.State, manga: Manga = manga()): Download =
    Download(source = mockk<HttpSource>(relaxed = true), manga = manga, chapter = chapter).apply {
        transition(state)
    }

/** Merged data holding [members] from the merged source. */
internal fun mergedData(vararg members: Manga): MergedMangaData = MergedMangaData(
    references = emptyList<MergedMangaReference>(),
    manga = members.associateBy { it.id },
    sources = emptyList(),
)

@RunWith(RobolectricTestRunner::class)
internal class MangaDownloadsTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() {
        mockkStatic(DELETION)
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L))
    }

    @After
    fun tearDown() {
        harness.stop()
        unmockkStatic(DELETION)
    }

    private fun emit(value: Download) = runBlocking { harness.statuses.emit(value) }

    private fun merged(model: MangaScreenModel, vararg members: Manga) {
        val source = mockk<MergedSource>(relaxed = true)
        model.updateSuccessState { it.copy(source = source, mergedData = mergedData(*members)) }
    }

    @Test
    fun queueUpdatesReachTheList() {
        val model = harness.loaded()
        eventually { harness.statuses.subscriptionCount.value == 2 }
        emit(download(chapter(2L), Download.State.QUEUE, manga().copy(id = 5L)))
        emit(download(chapter(1L), Download.State.DOWNLOADING))
        model.awaitSuccess { state -> state.chapters.any { it.downloadState == Download.State.DOWNLOADING } }
            .chapters
            .map { it.downloadState } shouldBe listOf(Download.State.DOWNLOADING, Download.State.NOT_DOWNLOADED)
    }

    @Test
    fun mergedQueueMatchesMembers() {
        val model = harness.loaded()
        merged(model, manga().copy(id = 5L))
        model.downloads.observe()
        eventually { harness.statuses.subscriptionCount.value == 4 }
        emit(download(chapter(1L), Download.State.DOWNLOADED, manga().copy(id = 5L)))
        model.awaitSuccess { state -> state.chapters.first().isDownloaded }
    }

    @Test
    fun queueErrorsAreLogged() {
        every { harness.downloadManager.statusFlow() } returns flow { error("status") }
        every { harness.downloadManager.progressFlow() } returns flow { error("progress") }
        harness.loaded().awaitSuccess().chapters.size shouldBe 2
    }

    @Test
    fun countsDownloads() {
        every { harness.downloadManager.getDownloadCount(any<Manga>()) } returnsMany listOf(0, 3)
        val model = harness.loaded()
        model.downloads.hasDownloads() shouldBe false
        model.downloads.hasDownloads() shouldBe true
        harness.loading().downloads.hasDownloads() shouldBe false
    }

    @Test
    fun deletesAllDownloads() {
        val model = harness.loaded()
        model.downloads.deleteDownloads()
        verify { harness.downloadManager.deleteManga(manga(favorite = true), any(), any()) }
        val member = manga().copy(id = 5L, source = 8L)
        merged(model, member)
        model.downloads.deleteDownloads()
        verify { harness.downloadManager.deleteManga(member, any(), any()) }
        model.updateSuccessState { it.copy(mergedData = null) }
        model.downloads.deleteDownloads()
        verify { harness.sourceManager.getOrStub(8L) }
        harness.loading().downloads.deleteDownloads()
    }

    @Test
    fun startNowNeedsOneChapter() {
        val model = harness.loaded()
        harness.queue.value = listOf(download(chapter(2L), Download.State.QUEUE))
        model.downloads.startDownload(listOf(chapter(1L), chapter(2L)), startNow = true)
        model.downloads.startDownload(listOf(chapter(2L)), startNow = true)
        eventually { harness.queue.value.size == 1 }
        harness.loading().downloads.startDownload(listOf(chapter(1L)), startNow = false)
    }

    @Test
    fun firstDownloadOffersLibrary() {
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        coEvery { harness.parts.getDuplicateLibraryManga(any()) } returns emptyList()
        val model = harness.loaded()
        model.downloads.startDownload(listOf(chapter(1L)), startNow = false)
        verify(timeout = 5_000) { harness.downloadManager.downloadChapters(any(), listOf(chapter(1L)), any()) }
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.performAction()
        coVerify(timeout = 5_000) { harness.updateManga.awaitUpdateFavorite(1L, true) }
        model.awaitSuccess().hasPromptedToAddBefore shouldBe true
        model.downloads.startDownload(listOf(chapter(1L)), startNow = false)
        verify(timeout = 5_000, exactly = 2) { harness.downloadManager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun dismissedOfferKeepsEntryOut() {
        harness.mangaFlow.value = manga() to listOf(chapter(1L))
        val model = harness.loaded()
        model.downloads.startDownload(listOf(chapter(1L)), startNow = false)
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        eventually { model.snackbarHostState.currentSnackbarData == null }
        coVerify(exactly = 0) { harness.updateManga.awaitUpdateFavorite(any(), any()) }
    }
}
