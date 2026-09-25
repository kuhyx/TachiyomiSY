package eu.kanade.domain.download.interactor

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.source.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal class DeleteDownloadTest {

    private val sourceManager = mockk<SourceManager>()
    private val downloadManager = mockk<DownloadManager>()
    private val interactor = DeleteDownload(sourceManager, downloadManager)
    private val manga = Manga.create().copy(id = 4, source = 7)
    private val chapter = Chapter.create().copy(id = 1)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun skipsUnknownSources() = runTest {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        every { sourceManager.get(7) } returns null
        interactor.awaitAll(manga, chapter)
        verify(exactly = 0) { downloadManager.deleteChapters(any(), any(), any()) }
    }

    @Test
    fun deletesViaDownloadManager() = runTest {
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        val source = mockk<Source>()
        every { sourceManager.get(7) } returns source
        every { downloadManager.deleteChapters(listOf(chapter), manga, source) } returns Unit
        interactor.awaitAll(manga, chapter)
        verify(exactly = 1) { downloadManager.deleteChapters(listOf(chapter), manga, source) }
    }
}
