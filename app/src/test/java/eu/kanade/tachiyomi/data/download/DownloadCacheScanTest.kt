package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadCacheScanTest : DownloadCacheTestBase() {

    private fun stub(name: String?, directory: Boolean, file: Boolean = !directory): UniFile = mockk<UniFile>().also {
        every { it.name } returns name
        every { it.isDirectory } returns directory
        every { it.isFile } returns file
    }

    @Test
    fun subDirsNeedNameAndDir() {
        val parent = mockk<UniFile>()
        every { parent.listFiles() } returns arrayOf(
            stub(name = "keep", directory = true),
            stub(name = null, directory = true),
            stub(name = " ", directory = true),
            stub(name = "file", directory = false),
        )
        parent.namedSubDirectories().map { it.name } shouldBe listOf("keep")
        val missing: UniFile? = null
        missing.namedSubDirectories() shouldBe emptyList()
        val unlisted = mockk<UniFile>()
        every { unlisted.listFiles() } returns null
        unlisted.namedSubDirectories() shouldBe emptyList()
    }

    @Test
    fun chapterNamesSkipTemp() {
        val mangaDir = mockk<UniFile>()
        every { mangaDir.listFiles() } returns arrayOf(
            stub(name = "Ch 1", directory = true),
            stub(name = "Ch 2.cbz", directory = false),
            stub(name = "Ch 3_tmp", directory = true),
            stub(name = "readme.txt", directory = false),
            stub(name = null, directory = true),
            stub(name = "odd", directory = false, file = false),
        )
        mangaDir.chapterDirNames() shouldBe mutableSetOf("Ch 1", "Ch 2")
        val missing: UniFile? = null
        missing.chapterDirNames() shouldBe mutableSetOf()
    }

    @Test
    fun scanFillsTheMangaDirectories() {
        entry(source = "Alpha", manga = "One", name = "Ch 1")
        entry(source = "Alpha", manga = "Two", name = "Ch 1.cbz", file = true)
        val sourceDir = SourceDirectory(uni(root.resolve("Alpha")))
        sourceDir.scan()
        sourceDir.mangaDirs.keys shouldBe setOf("One", "Two")
        sourceDir.chapterCount() shouldBe 2
        ("Ch 1" in sourceDir.mangaDirs.getValue("Two")) shouldBe true
        ("Ch 2" in sourceDir.mangaDirs.getValue("Two")) shouldBe false
    }

    @Test
    fun directoriesSurviveProtoBuf() {
        entry(source = "Alpha", manga = "One", name = "Ch 1")
        val rootDir = RootDirectory(uni(root))
        rootDir.sourceDirs = mapOf(1L to SourceDirectory(uni(root.resolve("Alpha"))).apply { scan() })
        val decoded = ProtoBuf.decodeFromByteArray<RootDirectory>(ProtoBuf.encodeToByteArray(rootDir))
        decoded.chapterCount() shouldBe 1
        decoded.dir!!.uri shouldBe uni(root).uri
    }

    @Test
    fun missingDirsSurviveProtoBuf() {
        val decoded = ProtoBuf.decodeFromByteArray<RootDirectory>(ProtoBuf.encodeToByteArray(RootDirectory(null)))
        decoded.dir shouldBe null
        decoded.chapterCount() shouldBe 0
        MangaDirectory(null).chapterDirs shouldBe mutableSetOf()
        val json = Json.encodeToString(RootDirectory.serializer(), RootDirectory(null))
        Json.decodeFromString(RootDirectory.serializer(), json).dir shouldBe null
    }
}
