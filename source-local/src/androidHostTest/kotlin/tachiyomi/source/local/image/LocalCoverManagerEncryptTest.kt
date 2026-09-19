package tachiyomi.source.local.image

import android.content.Context
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import tachiyomi.source.local.PNG_HEADER
import tachiyomi.source.local.ShadowZipWriter
import tachiyomi.source.local.ZIP_WRITER_PACKAGE
import tachiyomi.source.local.fileSystemOver
import tachiyomi.source.local.sampleManga
import java.io.File

/** The encrypted branch of [LocalCoverManager.update], which needs the shadowed `ZipWriter`. */
@RunWith(RobolectricTestRunner::class)
@Config(instrumentedPackages = [ZIP_WRITER_PACKAGE], shadows = [ShadowZipWriter::class])
internal class LocalCoverManagerEncryptTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        ShadowZipWriter.reset()
    }

    @Test
    fun updateEncryptsANewCover() {
        val dir = folder.newFolder("m")
        val manga = sampleManga("m")
        val manager = LocalCoverManager(context, fileSystemOver(folder.root))
        val written = manager.update(manga, PNG_HEADER.inputStream(), encrypted = true)
        written?.name shouldBe "cover.cbi"
        File(dir, "cover.cbi").isFile shouldBe true
        File(dir, ".nomedia").isFile shouldBe true
        ShadowZipWriter.lastTarget?.name shouldBe "cover.cbi"
        ShadowZipWriter.calls shouldBe listOf("open encrypt=true", "write cover.jpg ${PNG_HEADER.size}", "close")
        manga.thumbnail_url shouldEndWith "/m/cover.cbi"
    }

    @Test
    fun updateEncryptsTheOldCover() {
        val dir = folder.newFolder("m")
        File(dir, "cover.cbi").createNewFile()
        val manager = LocalCoverManager(context, fileSystemOver(folder.root))
        manager.update(sampleManga("m"), PNG_HEADER.inputStream(), encrypted = true)?.name shouldBe "cover.cbi"
        ShadowZipWriter.calls shouldBe listOf("open encrypt=true", "write cover.jpg ${PNG_HEADER.size}", "close")
    }
}
