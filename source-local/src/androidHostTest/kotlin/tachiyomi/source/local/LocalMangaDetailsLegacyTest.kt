package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.File

/** The legacy `details.json` migration of [LocalMangaDetails.fetch]. */
@RunWith(RobolectricTestRunner::class)
internal class LocalMangaDetailsLegacyTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)

    private fun details(fileSystem: LocalSourceFileSystem = fileSystemOver(folder.root)): LocalMangaDetails =
        LocalMangaDetails(
            context = context,
            fileSystem = fileSystem,
            coverManager = LocalCoverManager(context, fileSystem),
            json = testJson,
            comicInfoFiles = files,
        )

    private fun fetch(details: LocalMangaDetails): SManga {
        val manga = sampleManga("m")
        runTest { details.fetch(manga) shouldBe manga }
        return manga
    }

    // A manga folder that only lists [json], whose ComicInfo.xml is created by [createdFile].
    private fun mockedFileSystem(json: File, createdFile: UniFile?): LocalSourceFileSystem {
        val mangaDir = mockk<UniFile> {
            every { listFiles() } returns arrayOf(json.uni())
            every { createFile(COMIC_INFO_FILE) } returns createdFile
        }
        return mockk {
            every { getFilesInMangaDirectory("m") } returns emptyList()
            every { getMangaDirectory("m") } returns mangaDir
        }
    }

    @Test
    fun migratesEveryLegacyField() {
        val dir = folder.newFolder("m")
        val json = File(dir, "details.json")
        json.writeText(
            """{"title":"T","author":"A","artist":"R","description":"D","genre":["G1","G2"],"status":2,"extra":1}""",
        )
        val manga = fetch(details())
        manga.title shouldBe "T"
        manga.author shouldBe "A"
        manga.artist shouldBe "R"
        manga.description shouldBe "D"
        manga.genre shouldBe "G1, G2"
        manga.status shouldBe SManga.COMPLETED
        json.exists() shouldBe false
        val info = files.parse(File(dir, COMIC_INFO_FILE).inputStream())
        info.series?.value shouldBe "T"
        info.genre?.value shouldBe "G1, G2"
        info.publishingStatus?.value shouldBe "Completed"
    }

    @Test
    fun emptyLegacyJsonKeepsTheManga() {
        val dir = folder.newFolder("m")
        val json = File(dir, "details.json")
        json.writeText("{}")
        val manga = fetch(details())
        manga.title shouldBe "m"
        manga.author.shouldBeNull()
        manga.artist.shouldBeNull()
        manga.description.shouldBeNull()
        manga.genre.shouldBeNull()
        manga.status shouldBe SManga.UNKNOWN
        json.exists() shouldBe false
        files.parse(File(dir, COMIC_INFO_FILE).inputStream()).series?.value shouldBe "m"
    }

    @Test
    fun uncreatableComicInfoKeepsJson() {
        val json = folder.newFile("details.json")
        json.writeText("""{"title":"T"}""")
        fetch(details(mockedFileSystem(json, createdFile = null))).title shouldBe "T"
        json.exists() shouldBe true
    }
}
