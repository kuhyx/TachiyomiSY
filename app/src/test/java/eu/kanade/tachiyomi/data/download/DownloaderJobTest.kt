package eu.kanade.tachiyomi.data.download

import android.os.Looper
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.domain.category.model.Category
import java.io.File
import java.io.IOException

/** One download's job, the ComicInfo it writes, and the downloader's wiring. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderJobTest : DownloaderTestBase() {

    @Before
    fun setUpSource() {
        provider.downloadPreferences.saveChaptersAsCBZ.set(false)
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
        every { ImageUtil.getExtensionFromMimeType(any(), any()) } returns "png"
        coEvery { source.getPageList(any()) } returns readyPages(1)
        coEvery { source.getImage(any(), any()) } answers { imageResponse("png") }
        every { source.getChapterUrl(any()) } returns " https://source/c/1 "
    }

    private fun runJob(download: Download) = runBlocking {
        with(downloader) { scope.launchDownloadJob(download) }.join()
    }

    @Test
    fun finishedDownloadLeavesTheQueue() {
        val download = download(1L)
        downloader.addAllToQueue(listOf(download))
        runJob(download)
        downloader.queueState.value shouldBe emptyList()
        verify { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun failedDownloadStaysQueued() {
        coEvery { source.getPageList(any()) } throws IOException("offline")
        val failed = download(1L)
        val pending = download(2L)
        downloader.addAllToQueue(listOf(failed, pending))
        runJob(failed)
        downloader.queueState.value shouldBe listOf(failed, pending)
        verify(exactly = 0) { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun crashStopsTheDownloader() {
        File(root, "Source/Title").mkdirs()
        File(root, "Source/Title/Ch 1_tmp").writeText("a file where the directory goes")
        runJob(download(1L))
        (shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) != null) shouldBe true
        verify { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun cancelledJobIsQuiet() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        val job = with(downloader) { scope.launchDownloadJob(download(1L)) }
        Thread.sleep(200)
        runBlocking {
            job.cancel()
            job.join()
        }
        shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) shouldBe null
    }

    @Test
    fun comicInfoHasCategoriesAndUrls() = runBlocking<Unit> {
        coEvery { getCategories.await(1L) } returns listOf(Category(id = 1L, name = " Faves ", order = 0L, flags = 0L))
        coEvery { getTracks.await(1L) } returns listOf(
            domainTrack(remoteUrl = " https://tracker/1 "),
            domainTrack(remoteUrl = " "),
        )
        val dir = File(root, "info").apply { mkdirs() }
        File(dir, "ComicInfo.xml").writeText("old")
        val uni = com.hippo.unifile.UniFile.fromFile(dir)!!
        downloader.createComicInfoFile(uni, manga, chapter(1L), source)
        val xml = File(dir, "ComicInfo.xml").readText()
        (xml.contains("Faves") && xml.contains("https://tracker/1") && xml.contains("https://source/c/1")) shouldBe true
    }

    @Test
    fun savedQueueIsRestored() {
        // Let the harness's downloader finish its own (empty) restore first: restoring clears the store.
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        coEvery { getManga.await(1L) } returns manga
        coEvery { getChapter.await(7L) } returns chapter(7L)
        downloader.addAllToQueue(listOf(download(7L)))
        val restored = newDownloader()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        restored.queueState.value.map { it.chapter.id } shouldBe listOf(7L)
    }

    @Test
    fun defaultsComeFromInjekt() {
        loadKoinModules(
            module {
                single { chapterCache }
                single { provider.downloadPreferences }
                single { nl.adaptivity.xmlutil.serialization.XML.v1 {} }
                single { getCategories }
                single { getTracks }
                single { sourcePreferences }
            },
        )
        Downloader(context, provider.provider, cache).queueState.value shouldBe emptyList()
    }
}
