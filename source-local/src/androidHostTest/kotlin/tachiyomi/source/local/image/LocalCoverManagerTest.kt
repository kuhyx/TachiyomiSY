package tachiyomi.source.local.image

import android.content.Context
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.ClosableProbe
import tachiyomi.source.local.PNG_HEADER
import tachiyomi.source.local.fileSystemOver
import tachiyomi.source.local.sampleManga
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class LocalCoverManagerTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val manager by lazy { LocalCoverManager(context, fileSystemOver(folder.root)) }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // An unrecognised extension makes isImage sniff the (empty) content, which is not an image.
    private fun mangaFolder(vararg files: String): File = folder.newFolder("m").apply {
        files.forEach { name -> File(this, name).createNewFile() }
    }

    @Test
    fun findsTheFirstImageCalledCover() {
        val dir = mangaFolder("cover.bin")
        File(dir, "other.png").writeBytes(PNG_HEADER)
        File(dir, "cover").mkdir()
        File(dir, "COVER.jpg").writeBytes(PNG_HEADER)
        manager.find("m")?.name shouldBe "COVER.jpg"
    }

    @Test
    fun missingCoverGivesNothing() {
        mangaFolder("cover.bin", "page.png")
        manager.find("m").shouldBeNull()
        manager.find("missing").shouldBeNull()
    }

    @Test
    fun encryptedCoverArchiveCounts() {
        mangaFolder("cover.cbi")
        manager.find("m")?.name shouldBe "cover.cbi"
    }

    @Test
    fun coverArchiveCountsWithoutSniff() {
        mockkObject(ImageUtil)
        every { ImageUtil.isImage(any(), any()) } returns false
        mangaFolder("cover.cbi")
        manager.find("m")?.name shouldBe "cover.cbi"
    }

    @Test
    fun updateWritesANewCover() {
        val dir = mangaFolder()
        val manga = sampleManga("m")
        val written = manager.update(manga, PNG_HEADER.inputStream())
        written?.name shouldBe "cover.jpg"
        File(dir, "cover.jpg").readBytes() shouldBe PNG_HEADER
        File(dir, ".nomedia").isFile shouldBe true
        manga.thumbnail_url shouldBe written?.uri?.toString()
        manga.thumbnail_url shouldEndWith "/m/cover.jpg"
    }

    @Test
    fun updateOverwritesTheCover() {
        val dir = mangaFolder()
        File(dir, "cover.png").writeBytes(PNG_HEADER)
        val manga = sampleManga("m")
        val written = manager.update(manga, "new".byteInputStream(), encrypted = false)
        written?.name shouldBe "cover.png"
        File(dir, "cover.png").readText() shouldBe "new"
        manga.thumbnail_url shouldEndWith "/m/cover.png"
    }

    @Test
    fun updateFailsOnUncreatableCover() {
        File(mangaFolder(), "cover.jpg").mkdir()
        shouldThrow<IllegalStateException> { manager.update(sampleManga("m"), "x".byteInputStream()) }
    }

    @Test
    fun missingFolderClosesTheStream() {
        val stream = ClosableProbe()
        val manga = sampleManga("missing")
        manager.update(manga, stream).shouldBeNull()
        stream.isClosed shouldBe true
        manga.thumbnail_url.shouldBeNull()
    }
}
