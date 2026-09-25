package eu.kanade.tachiyomi.data.download

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class DownloadCacheTest : DownloadCacheTestBase() {

    private val manga = Manga.create().copy(id = 1L, source = 1L, ogTitle = "Title")

    private fun DownloadCache.isDownloaded(name: String, skipCache: Boolean = false) = isChapterDownloaded(
        chapterName = name,
        chapterScanlator = null,
        chapterUrl = "/c",
        mangaTitle = "Title",
        sourceId = 1L,
        skipCache = skipCache,
    )

    @Test
    fun renewIndexesTheTree() {
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        entry(source = "Alpha", manga = "Title", name = "Ch 2.cbz", file = true)
        entry(source = "Alpha", manga = "Title", name = "Ch 3_tmp")
        entry(source = "Alpha", manga = "Title", name = "notes.txt", file = true)
        entry(source = "Beta", manga = "Other", name = "Ch 1")
        File(root, "Unknown").mkdirs()
        val cache = newCache()
        waitUntil { cache.getTotalDownloadCount() == 2 }
        cache.isDownloaded("Ch 1") shouldBe true
        cache.isDownloaded("Ch 2") shouldBe true
        cache.isDownloaded("Ch 3") shouldBe false
        cache.getDownloadCount(manga) shouldBe 2
    }

    @Test
    fun stubSourcesAreIndexedToo() {
        entry(source = "Beta", manga = "Other", name = "Ch 1")
        val stub = mockk<StubSource>()
        every { stub.toString() } returns "Beta"
        every { stub.id } returns 2L
        every { sourceManager.getStubSources() } returns listOf(stub)
        val cache = newCache()
        waitUntil { cache.getTotalDownloadCount() == 1 }
    }

    @Test
    fun skipCacheAsksTheFilesystem() {
        val cache = newCache()
        cache.isDownloaded(name = "Ch 9", skipCache = true) shouldBe false
        entry(source = "Alpha", manga = "Title", name = "Ch 9")
        cache.isDownloaded(name = "Ch 9", skipCache = true) shouldBe true
    }

    @Test
    fun storageChangesRenewTheIndex() {
        val cache = newCache()
        waitUntil { cache.rootDownloadsDir.sourceDirs.isEmpty() && cache.getTotalDownloadCount() == 0 }
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        runBlocking { storageChanges.emit(Unit) }
        waitUntil { cache.getTotalDownloadCount() == 1 }
    }

    @Test
    fun indexIsSavedAndRestored() {
        entry(source = "Alpha", manga = "Title", name = "Ch 1")
        val first = newCache()
        waitUntil { first.getTotalDownloadCount() == 1 }
        waitUntil(timeoutMs = 10_000) { diskCacheFile.length() > 0 }
        File(root, "Alpha").deleteRecursively()
        val second = newCache()
        waitUntil { second.rootDownloadsDir.chapterCount() == 1 }
        second.getTotalDownloadCount() shouldBe 1
    }

    @Test
    fun corruptIndexIsDiscarded() {
        diskCacheFile.writeText("not protobuf")
        val cache = newCache()
        waitUntil { !diskCacheFile.exists() || diskCacheFile.readText() != "not protobuf" }
        cache.getTotalDownloadCount() shouldBe 0
    }

    @Test
    fun emptyRootHasNoDirectory() {
        every { provider.storageManager.getDownloadsDirectory() } returns null
        val bytes = ProtoBuf.encodeToByteArray(RootDirectory(null))
        diskCacheFile.writeBytes(bytes)
        val cache = newCache()
        waitUntil { cache.rootDownloadsDir.dir == null }
        cache.getTotalDownloadCount() shouldBe 0
    }

    @Test
    fun unwritableIndexIsLogged() {
        diskCacheFile.mkdirs()
        File(diskCacheFile, "child").writeText("x")
        val cache = newCache()
        cache.notifyChanges()
        runBlocking { cache.changes.first() }
        Thread.sleep(1_500)
        diskCacheFile.isDirectory shouldBe true
    }
}
