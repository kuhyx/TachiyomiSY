package tachiyomi.source.local

import android.content.Context
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import java.io.File

/** The encrypted branch of [ComicInfoFiles.copy], which needs the shadowed [mihon.core.common.archive.ZipWriter]. */
@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = [ZIP_WRITER_PACKAGE], shadows = [ShadowZipWriter::class])
internal class ComicInfoFilesEncryptTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val files = ComicInfoFiles(context, testXml)

    @Before
    fun setUp() {
        ShadowZipWriter.reset()
    }

    @Test
    fun copyEncryptsTheComicInfo() {
        val target = folder.newFolder("manga")
        val written = files.copy("<x/>".byteInputStream(), target.uni(), encrypt = true)
        written?.name shouldBe LocalSource.COMIC_INFO_ARCHIVE
        File(target, LocalSource.COMIC_INFO_ARCHIVE).isFile shouldBe true
        File(target, COMIC_INFO_FILE).exists() shouldBe false
        ShadowZipWriter.lastTarget?.name shouldBe LocalSource.COMIC_INFO_ARCHIVE
        ShadowZipWriter.calls shouldBe listOf("open encrypt=true", "write $COMIC_INFO_FILE 4", "close")
    }

    @Test
    fun uncreatableArchiveGivesNull() {
        val target = folder.newFolder("manga")
        File(target, LocalSource.COMIC_INFO_ARCHIVE).mkdir()
        files.copy("<x/>".byteInputStream(), target.uni(), encrypt = true).shouldBeNull()
        ShadowZipWriter.calls shouldBe emptyList()
    }
}
