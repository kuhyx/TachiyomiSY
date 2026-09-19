package tachiyomi.source.local.io

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.source.local.fileSystemOver
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class LocalSourceFileSystemTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun unsetStorageGivesNothing() {
        val fileSystem = fileSystemOver(null)
        fileSystem.getBaseDirectory().shouldBeNull()
        fileSystem.getFilesInBaseDirectory() shouldBe emptyList()
        fileSystem.getMangaDirectory("manga").shouldBeNull()
        fileSystem.getFilesInMangaDirectory("manga") shouldBe emptyList()
    }

    @Test
    fun listsTheBaseDirectory() {
        folder.newFolder("manga")
        folder.newFile("stray.txt")
        val fileSystem = fileSystemOver(folder.root)
        fileSystem.getBaseDirectory()?.filePath shouldBe folder.root.path
        fileSystem.getFilesInBaseDirectory().map { it.name }.toSet() shouldBe setOf("manga", "stray.txt")
    }

    @Test
    fun mangaDirectoryMustBeAFolder() {
        val manga = folder.newFolder("manga")
        File(manga, "ch1.cbz").createNewFile()
        folder.newFile("stray.txt")
        val fileSystem = fileSystemOver(folder.root)
        fileSystem.getMangaDirectory("manga")?.filePath shouldBe manga.path
        fileSystem.getMangaDirectory("stray.txt").shouldBeNull()
        fileSystem.getMangaDirectory("missing").shouldBeNull()
        fileSystem.getFilesInMangaDirectory("manga").map { it.name } shouldBe listOf("ch1.cbz")
        fileSystem.getFilesInMangaDirectory("stray.txt") shouldBe emptyList()
        fileSystem.getFilesInMangaDirectory("missing") shouldBe emptyList()
    }
}
