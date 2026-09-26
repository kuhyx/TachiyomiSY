package eu.kanade.tachiyomi.data.download

import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class DownloaderChapterTest : DownloaderTestBase() {

    @Test
    fun missingDownloadsDirFails() {
        every { provider.storageManager.getDownloadsDirectory() } returns null
        val download = download(1L)
        downloader.mangaDirWithSpace(download).shouldBeNull()
        download.status shouldBe Download.State.ERROR
        (shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) != null) shouldBe true
    }

    @Test
    fun fullDiskFails() {
        val mangaDir = File(root, "Source/Title").apply { mkdirs() }
        ShadowStatFs.registerStats(mangaDir, 10, 10, 10)
        val download = download(1L)
        downloader.mangaDirWithSpace(download).shouldBeNull()
        download.status shouldBe Download.State.ERROR
    }

    @Test
    fun roomyDiskIsUsed() {
        downloader.mangaDirWithSpace(download(1L))!!.name shouldBe "Title"
    }

    @Test
    fun unknownSpaceIsUsedAnyway() {
        val mangaDir = mockk<UniFile>(relaxed = true)
        every { mangaDir.uri } returns Uri.parse("opaque:space")
        val sourceDir = mockk<UniFile>(relaxed = true)
        every { sourceDir.createDirectory(any()) } returns mangaDir
        val downloads = mockk<UniFile>(relaxed = true)
        every { downloads.createDirectory(any()) } returns sourceDir
        every { provider.storageManager.getDownloadsDirectory() } returns downloads
        downloader.mangaDirWithSpace(download(1L)) shouldBe mangaDir
    }

    @Test
    fun noPagesFinishAtOnce() = runTest(UnconfinedTestDispatcher()) {
        sourcePreferences.dataSaverDownloader.set(false)
        val dir = UniFile.fromFile(File(root, "empty").apply { mkdirs() })!!
        downloader.downloadPages(download(1L), emptyList(), dir)
        dir.listFiles()!!.size shouldBe 0
    }

    @Test
    fun emptyPageListFails() = runTest {
        coEvery { source.getPageList(any()) } returns emptyList()
        shouldThrow<IOException> { downloader.fetchPageList(download(1L)) }
    }

    @Test
    fun pageListIsReindexed() = runTest {
        coEvery { source.getPageList(any()) } returns listOf(Page(5, url = "/a"), Page(9, imageUrl = "https://i/b"))
        val download = download(1L)
        downloader.fetchPageList(download).map { it.index } shouldBe listOf(0, 1)
        download.pages!!.map { it.imageUrl } shouldBe listOf(null, "https://i/b")
    }
}
