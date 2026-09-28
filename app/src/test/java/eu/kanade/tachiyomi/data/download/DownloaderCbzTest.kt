package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.core.common.archive.ZipWriter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** CBZ output with libarchive stubbed: the archive replaces the page folder, whatever that folder holds. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderCbzTest : DownloaderPipelineBase() {

    private val mangaDir by lazy { requireNotNull(UniFile.fromFile(File(root, "Source/Title").apply { mkdirs() })) }

    @Before
    fun stubArchive() {
        stubLibArchive()
        mockkObject(CbzCrypto)
        every { CbzCrypto.getPasswordProtectDlPref() } returns false
    }

    @Test
    fun pagesAreArchivedAndFolderGoes() {
        val tmpDir = requireNotNull(mangaDir.createDirectory("Ch 1_tmp"))
        requireNotNull(tmpDir.createFile("001.png")).openOutputStream().use { it.write(1) }
        downloader.archiveChapter(mangaDir, "Ch 1", tmpDir)
        verify(exactly = 1) { anyConstructed<ZipWriter>().write(match<UniFile> { it.name == "001.png" }) }
        File(root, "Source/Title/Ch 1.cbz").exists() shouldBe true
        File(root, "Source/Title/Ch 1_tmp").exists() shouldBe false
    }

    @Test
    fun aFolderThatIsAFileArchives() {
        val notADir = requireNotNull(mangaDir.createFile("Ch 2_tmp"))
        downloader.archiveChapter(mangaDir, "Ch 2", notADir)
        File(root, "Source/Title/Ch 2.cbz").exists() shouldBe true
    }

    @Test
    fun cbzDownloadFinishes() = runTest {
        provider.downloadPreferences.saveChaptersAsCBZ.set(true)
        val download = download(1L)
        downloader.downloadChapter(download)
        download.shouldBeDownloaded()
    }
}
