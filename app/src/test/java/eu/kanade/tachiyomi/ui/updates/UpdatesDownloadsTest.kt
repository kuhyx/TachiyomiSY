package eu.kanade.tachiyomi.ui.updates

import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.startDownloadNow
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class UpdatesDownloadsTest {
    private val harness = UpdatesHarness()
    private val chapter = Chapter.create().copy(id = 1)
    private val manga = Manga.create().copy(id = 1, source = 5)
    private lateinit var model: UpdatesScreenModel

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(harness.koinModules()) }
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        every { harness.downloadManager.startDownloads() } just runs
        every { harness.downloadManager.startDownloadNow(any()) } just runs
        every { harness.downloadManager.cancelQueuedDownloads(any()) } just runs
        every { harness.downloadManager.deleteChapters(any(), any(), any()) } just runs
        coEvery { harness.getChapter.await(any()) } returns chapter
        coEvery { harness.getManga.await(1L) } returns manga
        every { harness.sourceManager.get(5L) } returns mockk<Source>()
        harness.updates.value = listOf(update(1), update(2))
        model = harness.model()
        model.state.await { it.items.size == 2 }
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun statusChangesReachTheItem() {
        model.updateDownloadState(download(2, Download.State.DOWNLOADING))
        model.updateDownloadState(download(9, Download.State.DOWNLOADED))
        val items = model.state.value.items
        items.map { it.downloadStateProvider() } shouldBe
            listOf(Download.State.NOT_DOWNLOADED, Download.State.DOWNLOADING)
        items[1].downloadProgressProvider() shouldBe 0
    }

    @Test
    fun startDownloadsTheChapters() {
        model.downloadChapters(emptyList(), ChapterDownloadAction.START)
        model.downloadChapters(listOf(item(1)), ChapterDownloadAction.START)
        verify(timeout = WAIT) { harness.downloadManager.downloadChapters(manga, listOf(chapter), any()) }
        verify(exactly = 0) { harness.downloadManager.startDownloads() }
        model.downloadChapters(listOf(item(1, state = Download.State.ERROR)), ChapterDownloadAction.START)
        verify(timeout = WAIT) { harness.downloadManager.startDownloads() }
    }

    @Test
    fun unavailableMangaIsSkipped() {
        coEvery { harness.getManga.await(2L) } returns manga.copy(id = 2, source = 6)
        coEvery { harness.getManga.await(3L) } returns null
        every { harness.sourceManager.get(6L) } returns null
        model.downloadChapters(listOf(item(3, mangaId = 2), item(4, mangaId = 3)))
        verify(timeout = WAIT) { harness.sourceManager.get(6L) }
        verify(exactly = 0) { harness.downloadManager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun startNowNeedsOneItem() {
        model.downloadChapters(listOf(item(1), item(2)), ChapterDownloadAction.START_NOW)
        model.downloadChapters(listOf(item(1)), ChapterDownloadAction.START_NOW)
        verify(exactly = 1) { harness.downloadManager.startDownloadNow(1L) }
    }

    @Test
    fun cancelNeedsAQueuedDownload() {
        val queued = download(1, Download.State.DOWNLOADING)
        harness.queue.value = listOf(queued)
        model.downloadChapters(listOf(item(1), item(2)), ChapterDownloadAction.CANCEL)
        model.downloadChapters(listOf(item(2)), ChapterDownloadAction.CANCEL)
        model.downloadChapters(listOf(item(1)), ChapterDownloadAction.CANCEL)
        verify(exactly = 1) { harness.downloadManager.cancelQueuedDownloads(listOf(queued)) }
        queued.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun deleteRemovesTheChapters() {
        model.toggleAllSelection(true)
        model.downloadChapters(listOf(item(1)), ChapterDownloadAction.DELETE)
        verify(timeout = WAIT) { harness.downloadManager.deleteChapters(listOf(chapter), manga, any()) }
        model.state.value.selectionMode shouldBe false
    }
}

private const val WAIT = 5_000L
