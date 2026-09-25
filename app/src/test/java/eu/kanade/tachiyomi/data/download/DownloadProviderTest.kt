package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class DownloadProviderTest : ProviderTestBase() {

    @Test
    fun mangaDirIsCreatedUnderSource() {
        val dir = harness.provider.getMangaDir(mangaTitle = "Title", source = source).getOrThrow()
        dir.name shouldBe "Title"
        File(root, "Source/Title").isDirectory shouldBe true
    }

    @Test
    fun missingDownloadsDirFails() {
        val without = DownloadProviderHarness(root = null, context = context)
        val failure = without.provider.getMangaDir(mangaTitle = "Title", source = source)
        failure.isFailure shouldBe true
        (failure.exceptionOrNull() is IOException) shouldBe true
    }

    @Test
    fun unwritableSourceDirFails() {
        val stubbed = mockk<UniFile>()
        every { stubbed.createDirectory(any()) } returns null
        every { stubbed.filePath } returns "/nope"
        every { stubbed.uri } returns UniFile.fromFile(root)!!.uri
        every { harness.storageManager.getDownloadsDirectory() } returns stubbed
        harness.provider.getMangaDir(mangaTitle = "Title", source = source).isFailure shouldBe true
    }

    @Test
    fun unwritableMangaDirFails() {
        val sourceDir = mockk<UniFile>()
        every { sourceDir.createDirectory(any()) } returns null
        every { sourceDir.filePath } returns "/nope/Source"
        every { sourceDir.uri } returns UniFile.fromFile(root)!!.uri
        val downloads = mockk<UniFile>()
        every { downloads.createDirectory(any()) } returns sourceDir
        every { harness.storageManager.getDownloadsDirectory() } returns downloads
        harness.provider.getMangaDir(mangaTitle = "Title", source = source).isFailure shouldBe true
    }

    /** A document provider may list a file without a name; it is neither temporary nor kept. */
    @Test
    fun namelessFileIsNotUnmatched() {
        val nameless = mockk<UniFile>()
        every { nameless.name } returns null
        val mangaDir = mockk<UniFile>()
        every { mangaDir.listFiles() } returns arrayOf(nameless)
        every { mangaDir.findFile(any()) } returns nameless
        val sourceDir = mockk<UniFile>()
        every { sourceDir.findFile("Title") } returns mangaDir
        val downloads = mockk<UniFile>()
        every { downloads.findFile("Source") } returns sourceDir
        every { harness.storageManager.getDownloadsDirectory() } returns downloads
        val unmatched = harness.provider.findUnmatchedChapterDirs(
            chapters = listOf(testChapter(name = "Ch 1")),
            manga = testManga("Title"),
            source = source,
        )
        unmatched.isEmpty() shouldBe true
    }

    @Test
    fun defaultsComeFromInjekt() {
        startKoin {
            modules(
                module {
                    single { harness.storageManager }
                    single { harness.libraryPreferences }
                    single { harness.downloadPreferences }
                },
            )
        }
        try {
            DownloadProvider(context).getMangaDir(mangaTitle = "Injekt", source = source).isSuccess shouldBe true
        } finally {
            stopKoin()
        }
    }
}
