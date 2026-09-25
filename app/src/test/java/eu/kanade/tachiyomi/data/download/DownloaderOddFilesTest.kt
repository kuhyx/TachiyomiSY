package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Page
import exh.util.DataSaver
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil

/**
 * Directory listings a real filesystem never produces but a document provider can: entries
 * without a name, and a directory that cannot be listed at all.
 */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderOddFilesTest : DownloaderTestBase() {

    private val nameless: UniFile = mockk<UniFile>(relaxed = true).also { every { it.name } returns null }
    private val listing: UniFile = mockk<UniFile>(relaxed = true).also {
        every { it.listFiles() } returns arrayOf(nameless)
        every { it.findFile(any()) } returns null
        every { it.createFile(any()) } returns null
    }
    private val unlistable: UniFile = mockk<UniFile>(relaxed = true).also { every { it.listFiles() } returns null }

    @Before
    fun setUpImages() {
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
    }

    @Test
    fun namelessEntriesAreNotPages() = runTest {
        val page = Page(0, imageUrl = "https://i/1.png")
        downloader.getOrDownloadImage(page, download(1L), listing, DataSaver.NoOp)
        (page.status is Page.State.Error) shouldBe true
    }

    @Test
    fun unlistableDirectoryHasNoPages() = runTest {
        val page = Page(0, imageUrl = "https://i/1.png")
        downloader.getOrDownloadImage(page, download(1L), unlistable, DataSaver.NoOp)
        (page.status is Page.State.Error) shouldBe true
    }

    @Test
    fun namelessEntriesCountAsImages() {
        val download = download(1L).apply { pages = listOf(Page(0).apply { status = Page.State.Ready }) }
        downloader.isDownloadSuccessful(download, listing) shouldBe true
        downloader.isDownloadSuccessful(download, unlistable) shouldBe false
    }

    @Test
    fun splittingNeedsAPageFile() {
        provider.downloadPreferences.splitTallImages.set(true)
        downloader.splitTallImageIfNeeded(Page(0), listing)
        downloader.splitTallImageIfNeeded(Page(0), unlistable)
        verify(exactly = 0) { ImageUtil.splitTallImage(any(), any(), any()) }
    }
}
