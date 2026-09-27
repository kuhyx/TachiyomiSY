package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Renaming source and manga folders, including case-only renames that go through a temporary name. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerRenamesTest : DownloadManagerTestBase() {

    private fun dir(path: String, withFile: Boolean = false) = File(root, path).apply {
        mkdirs()
        if (withFile) File(this, "keep").writeText("x")
    }

    private fun failures() = logged.count { it.startsWith("Failed to rename") }

    @Test
    fun sourceWithoutFolderIsSkipped() {
        manager.renameSource(source, httpSource(name = "Other", id = 6L))
        File(root, "Other").exists() shouldBe false
    }

    @Test
    fun sourceFolderIsRenamed() {
        dir("Source")
        manager.renameSource(source, httpSource(name = "Source", id = 6L))
        manager.renameSource(source, httpSource(name = "Other", id = 6L))
        File(root, "Other").exists() shouldBe true
        File(root, "Source").exists() shouldBe false
    }

    @Test
    fun sourceCaseChangeUsesTempName() {
        dir("Source")
        manager.renameSource(source, httpSource(name = "SOURCE", id = 6L))
        root.list()?.toList() shouldBe listOf("SOURCE")
    }

    @Test
    fun sourceRenameFailuresAreLogged() {
        dir("Source")
        dir("SOURCE_tmp", withFile = true)
        manager.renameSource(source, httpSource(name = "SOURCE", id = 6L))
        dir("Other", withFile = true)
        manager.renameSource(source, httpSource(name = "Other", id = 6L))
        failures() shouldBe 2
        File(root, "Source").exists() shouldBe true
    }

    @Test
    fun mangaFolderIsRenamed() = runTest {
        manager.renameManga(manga, "New")
        dir("Source/Title")
        manager.renameManga(manga, "Title")
        manager.renameManga(manga, "New")
        File(root, "Source/New").exists() shouldBe true
        coVerify { cache.renameManga(manga, any(), "New") }
    }

    @Test
    fun mangaCaseChangeUsesTempName() = runTest {
        dir("Source/Title")
        manager.renameManga(manga, "TITLE")
        File(root, "Source").list()?.toList() shouldBe listOf("TITLE")
    }

    @Test
    fun mangaRenameFailureIsLogged() = runTest {
        dir("Source/Title")
        dir("Source/New", withFile = true)
        manager.renameManga(manga, "New")
        dir("Source/TITLE_tmp", withFile = true)
        manager.renameManga(manga, "TITLE")
        failures() shouldBe 2
        coVerify(exactly = 0) { cache.renameManga(any(), any(), any()) }
    }

    @Test
    fun mangaDirRenameNeedsBoth() {
        every { sourceManager.getOrStub(9L) } returns httpSource(name = "Nine", id = 9L)
        manager.renameMangaDir("Title", "New", 9L)
        manager.renameMangaDir("Title", "New", 5L)
        dir("Source")
        manager.renameMangaDir("Title", "New", 5L)
        File(root, "Source/New").exists() shouldBe false
        dir("Source/Title")
        manager.renameMangaDir("Title", "New", 5L)
        File(root, "Source/New").exists() shouldBe true
    }
}
