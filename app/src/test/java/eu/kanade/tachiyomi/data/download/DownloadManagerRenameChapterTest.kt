package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Renaming one downloaded chapter after a source sync renamed it. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerRenameChapterTest : DownloadManagerTestBase() {

    private fun renamed(id: Long, name: String) = chapter(id).copy(name = name)

    @Test
    fun folderFollowsTheNewName() = runTest {
        downloaded(chapter(1))
        manager.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        File(root, "Source/Title/Ch One").exists() shouldBe true
        coVerify { cache.removeChapter(chapter(1), manga) }
        coVerify { cache.addChapter("Ch One", any(), manga) }
    }

    @Test
    fun archiveKeepsItsExtension() = runTest {
        File(root, "Source/Title").mkdirs()
        File(root, "Source/Title/Ch 1.cbz").writeText("zip")
        manager.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        File(root, "Source/Title/Ch One.cbz").exists() shouldBe true
    }

    @Test
    fun plainFileKeepsItsName() = runTest {
        File(root, "Source/Title").mkdirs()
        File(root, "Source/Title/Ch 1").writeText("not a folder")
        manager.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        File(root, "Source/Title/Ch One").isFile shouldBe true
    }

    @Test
    fun sameNameIsLeftAlone() = runTest {
        downloaded(chapter(1))
        manager.renameChapter(source, manga, chapter(1), chapter(1))
        coVerify(exactly = 0) { cache.removeChapter(any(), any()) }
    }

    @Test
    fun missingChapterIsSkipped() = runTest {
        downloaded(chapter(2))
        manager.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        File(root, "Source/Title/Ch One").exists() shouldBe false
    }

    @Test
    fun failedRenameIsLogged() = runTest {
        downloaded(chapter(1))
        downloaded(renamed(1, "Ch One"))
        manager.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        logged.any { it.startsWith("Could not rename downloaded chapter") } shouldBe true
        coVerify(exactly = 0) { cache.removeChapter(any(), any()) }
    }

    @Test
    fun noDownloadsFolderIsLogged() = runTest {
        val detached = DownloadManager(
            context = context,
            provider = DownloadProviderHarness(root = null, context = context).provider,
            cache = cache,
            getCategories = getCategories,
            sourceManager = sourceManager,
            downloadPreferences = provider.downloadPreferences,
        )
        try {
            detached.renameChapter(source, manga, chapter(1), renamed(1, "Ch One"))
        } finally {
            detached.downloader.scope.cancel()
        }
        logged.any { it.startsWith("Manga download folder doesn't exist") } shouldBe true
    }
}
