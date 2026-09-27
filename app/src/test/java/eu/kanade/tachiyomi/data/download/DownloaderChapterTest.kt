package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

/** One chapter end to end: page list, images (from the reader cache), ComicInfo and the final rename. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderChapterTest : DownloaderPipelineBase() {

    private fun chapterDir(): File = File(root, "Source/Title/Ch 1")

    @Test
    fun chapterIsWrittenAndCached() = runTest {
        val download = download(1L)
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
        chapterDir().list().orEmpty().sorted() shouldBe listOf(".nomedia", "001.png", "002.png", "ComicInfo.xml")
        download.pages?.map { it.index } shouldBe listOf(0, 1)
        coVerify { cache.addChapter("Ch 1", any(), manga) }
    }

    @Test
    fun emptyPageListFails() = runTest {
        coEvery { source.getPageList(any()) } returns emptyList()
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
        chapterDir().exists() shouldBe false
    }

    @Test
    fun knownPagesAreNotRefetched() = runTest {
        val download = download(1L).apply { pages = listOf(Page(0, imageUrl = "https://i/1")) }
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
        coVerify(exactly = 0) { source.getPageList(any()) }
    }

    @Test
    fun missingUrlsAreResolved() = runTest {
        val download = download(1L).apply { pages = listOf(Page(0, imageUrl = ""), Page(1)) }
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
    }

    @Test
    fun unresolvableUrlFailsTheChapter() = runTest {
        coEvery { source.getImageUrl(any()) } throws IOException("no url")
        val download = download(1L).apply { pages = listOf(Page(0, imageUrl = "https://i/1"), Page(1)) }
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
        (download.pages?.last()?.status is Page.State.Error) shouldBe true
    }

    @Test
    fun dataSaverSettingIsHonoured() = runTest {
        sourcePreferences.dataSaverDownloader.set(true)
        val download = download(1L)
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
    }

    @Test
    fun fullDiskFailsEarly() = runTest {
        space = 1L
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
        coVerify(exactly = 0) { source.getPageList(any()) }
        space = -1L
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
    }

    @Test
    fun missingMangaDirFails() = runTest {
        val broken = mockk<DownloadProvider>()
        every { broken.getMangaDir(any(), any()) } returns Result.failure(IOException("no dir"))
        val download = download(1L)
        val other = newDownloaderWith(broken)
        try {
            other.downloadChapter(download)
        } finally {
            other.scope.cancel()
        }
        download.status shouldBe Download.State.ERROR
    }
}
