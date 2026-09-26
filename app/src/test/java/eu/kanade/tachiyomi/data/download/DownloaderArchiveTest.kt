package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File

/** Chapters saved as CBZ, over libarchive whose JNI is stubbed by Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = [LIBARCHIVE_PACKAGE], shadows = [ShadowArchive::class, ShadowArchiveEntry::class])
internal class DownloaderArchiveTest : DownloaderTestBase() {

    @Before
    fun setUpSource() {
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
        every { ImageUtil.getExtensionFromMimeType(any(), any()) } returns "png"
        coEvery { source.getPageList(any()) } returns readyPages(1)
        coEvery { source.getImage(any(), any()) } answers { imageResponse("png") }
        every { source.getChapterUrl(any()) } returns "https://source/c/1"
        // CbzCrypto resolves its preferences once per JVM, so its switches are stubbed, not set.
        mockkObject(CbzCrypto)
        every { CbzCrypto.getPasswordProtectDlPref() } returns false
        every { CbzCrypto.isPasswordSet() } returns false
    }

    private fun archive(): Pair<File, File> {
        val mangaDir = File(root, "manga").apply { mkdirs() }
        val tmp = File(mangaDir, "Ch 1_tmp").apply { mkdirs() }
        File(tmp, "001.png").writeText("png")
        downloader.archiveChapter(UniFile.fromFile(mangaDir)!!, "Ch 1", UniFile.fromFile(tmp)!!)
        return mangaDir to tmp
    }

    @Test
    fun chapterBecomesACbz() {
        val (mangaDir, tmp) = archive()
        File(mangaDir, "Ch 1.cbz").exists() shouldBe true
        tmp.exists() shouldBe false
    }

    @Test
    fun noPasswordStaysPlain() {
        every { CbzCrypto.getPasswordProtectDlPref() } returns true
        archive().first.resolve("Ch 1.cbz").exists() shouldBe true
    }

    @Test
    fun protectionWithPasswordEncrypts() {
        every { CbzCrypto.getPasswordProtectDlPref() } returns true
        every { CbzCrypto.isPasswordSet() } returns true
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "secret".toByteArray()
        archive().first.resolve("Ch 1.cbz").exists() shouldBe true
    }

    @Test
    fun unlistableTmpMakesEmptyCbz() {
        val mangaDir = File(root, "manga").apply { mkdirs() }
        val tmp = mockk<UniFile>(relaxed = true)
        every { tmp.listFiles() } returns null
        downloader.archiveChapter(UniFile.fromFile(mangaDir)!!, "Ch 2", tmp)
        File(mangaDir, "Ch 2.cbz").exists() shouldBe true
    }

    @Test
    fun cbzDownloadsFinish() = runTest {
        provider.downloadPreferences.saveChaptersAsCBZ.set(true)
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.DOWNLOADED
        File(root, "Source/Title/Ch 1.cbz").exists() shouldBe true
    }
}
