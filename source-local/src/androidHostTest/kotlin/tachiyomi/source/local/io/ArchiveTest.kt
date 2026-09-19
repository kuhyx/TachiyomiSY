package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.source.local.uni

@RunWith(RobolectricTestRunner::class)
internal class ArchiveTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun everySupportedExtensionCounts() {
        listOf("zip", "cbz", "rar", "cbr", "7z", "cb7", "tar", "cbt").forEach { extension ->
            Archive.isSupported(folder.newFile("chapter.$extension").uni()) shouldBe true
        }
        Archive.isSupported(folder.newFile("upper.CBZ").uni()) shouldBe true
    }

    @Test
    fun otherFilesDoNotCount() {
        Archive.isSupported(folder.newFile("notes.txt").uni()) shouldBe false
        Archive.isSupported(folder.newFile("noextension").uni()) shouldBe false
        Archive.isSupported(folder.newFolder("chapter.cbz.d").uni()) shouldBe false
    }

    @Test
    fun namelessFileDoesNotCount() {
        val nameless = mockk<UniFile> { every { name } returns null }
        Archive.isSupported(nameless) shouldBe false
    }
}
