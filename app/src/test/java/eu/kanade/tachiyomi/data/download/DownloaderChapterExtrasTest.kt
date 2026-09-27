package eu.kanade.tachiyomi.data.download

import android.os.Looper
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.track.model.Track
import java.io.File

/** The ComicInfo a chapter carries, the data-saver and CBZ settings, and the queue the downloader restores. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderChapterExtrasTest : DownloaderPipelineBase() {

    private fun track(url: String) = Track(
        id = 1L,
        mangaId = 1L,
        trackerId = 2L,
        remoteId = 3L,
        libraryId = null,
        title = "t",
        lastChapterRead = 0.0,
        totalChapters = 0L,
        status = 0L,
        score = 0.0,
        remoteUrl = url,
        startDate = 0L,
        finishDate = 0L,
        private = false,
    )

    @Test
    fun comicInfoNamesShelfAndTracks() = runTest {
        coEvery { getCategories.await(any()) } returns listOf(Category(id = 1, name = " Shelf ", order = 0, flags = 0))
        coEvery { getTracks.await(any<Long>()) } returns listOf(track(" "), track("https://tracker/1 "))
        val download = download(1L)
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
        val comicInfo = File(root, "Source/Title/Ch 1/$COMIC_INFO_FILE").readText()
        comicInfo shouldContain "Shelf"
        comicInfo shouldContain "https://tracker/1"
    }

    @Test
    fun comicInfoIsRewritten() = runTest {
        val dir = requireNotNull(UniFile.fromFile(File(root, "info").apply { mkdirs() }))
        downloader.createComicInfoFile(dir, manga, chapter(1), source)
        downloader.createComicInfoFile(dir, manga, chapter(2), source)
        File(root, "info").list()?.toList() shouldBe listOf(COMIC_INFO_FILE)
    }

    @Test
    fun dataSaverCanBeOff() = runTest {
        sourcePreferences.dataSaverDownloader.set(false)
        val download = download(1L)
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
    }

    @Test
    fun cbzOutputNeedsTheNativeWriter() = runTest {
        provider.downloadPreferences.saveChaptersAsCBZ.set(true)
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
    }

    @Test
    fun restoredQueueStartsEmpty() {
        shadowOf(Looper.getMainLooper()).idle()
        downloader.queueState.value shouldBe emptyList()
    }
}
