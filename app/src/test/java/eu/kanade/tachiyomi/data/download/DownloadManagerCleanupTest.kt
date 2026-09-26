package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.online.installSilentXLog
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** [cleanupChapters]: unmatched folders, read chapters and manga that left the library. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerCleanupTest : DownloadManagerTestBase() {

    @Before
    fun silenceXLog() {
        installSilentXLog()
    }

    private fun dir(path: String): File = File(root, path).apply { mkdirs() }

    private suspend fun cleanup(removeRead: Boolean, removeNonFavorite: Boolean, favorite: Boolean = true) =
        manager.cleanupChapters(
            allChapters = listOf(chapter(1L), chapter(2L).copy(read = true)),
            manga = manga.copy(favorite = favorite),
            source = source,
            removeRead = removeRead,
            removeNonFavorite = removeNonFavorite,
        )

    @Test
    fun nonFavoriteMangaIsDropped() = runTest {
        dir("Source/Title/Ch 1")
        dir("Source/Title/Ch 2")
        cleanup(removeRead = false, removeNonFavorite = true, favorite = false) shouldBe 3
        File(root, "Source/Title").exists() shouldBe false
    }

    @Test
    fun unmatchedAndReadFoldersGo() = runTest {
        every { cache.getDownloadCount(any()) } returns 1
        dir("Source/Title/Ch 1")
        dir("Source/Title/Ch 2")
        dir("Source/Title/Ch 9_tmp")
        cleanup(removeRead = true, removeNonFavorite = true) shouldBe 2
        File(root, "Source/Title").list()!!.toList() shouldBe listOf("Ch 1")
        coVerify { cache.removeFolders(listOf("Ch 9_tmp"), any()) }
    }

    @Test
    fun readFoldersStayWhenAsked() = runTest {
        every { cache.getDownloadCount(any()) } returns 1
        dir("Source/Title/Ch 2")
        cleanup(removeRead = false, removeNonFavorite = false) shouldBe 0
        File(root, "Source/Title/Ch 2").isDirectory shouldBe true
    }

    @Test
    fun emptyCacheDropsALeftoverFolder() = runTest {
        every { cache.getDownloadCount(any()) } returns 0
        dir("Source/Title/stray.txt")
        dir("Source/Title/Ch 1")
        cleanup(removeRead = false, removeNonFavorite = false)
        File(root, "Source/Title").exists() shouldBe false
        coVerify { cache.removeManga(any()) }
    }

    @Test
    fun emptyCacheEmptyFolderLogs() = runTest {
        every { cache.getDownloadCount(any()) } returns 0
        cleanup(removeRead = false, removeNonFavorite = false) shouldBe 0
        File(root, "Source/Title").isDirectory shouldBe true
    }

    @Test
    fun noDownloadsDirectory() = runTest {
        every { provider.storageManager.getDownloadsDirectory() } returns null
        every { cache.getDownloadCount(any()) } returns 0
        cleanup(removeRead = true, removeNonFavorite = true, favorite = false) shouldBe 0
    }

    @Test
    fun mangaFileInsteadOfFolder() = runTest {
        every { cache.getDownloadCount(any()) } returns 0
        dir("Source")
        File(root, "Source/Title").writeText("x")
        cleanup(removeRead = false, removeNonFavorite = false) shouldBe 0
        File(root, "Source/Title").isFile shouldBe true
    }

    @Test
    fun unlistableMangaFolderOnlyLogs() = runTest {
        every { cache.getDownloadCount(any()) } returns 0
        val mangaDir = mockk<UniFile> { every { listFiles() } returns null }
        val sourceDir = mockk<UniFile> { every { createDirectory(any()) } returns mangaDir }
        val downloads = mockk<UniFile>(relaxed = true) { every { createDirectory(any()) } returns sourceDir }
        every { provider.storageManager.getDownloadsDirectory() } returns downloads
        cleanup(removeRead = false, removeNonFavorite = false) shouldBe 0
    }
}
