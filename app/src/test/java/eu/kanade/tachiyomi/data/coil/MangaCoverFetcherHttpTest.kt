package eu.kanade.tachiyomi.data.coil

import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** [httpLoader]: the library cover file, then Coil's disk cache, then the network. */
internal class MangaCoverFetcherHttpTest {

    @TempDir
    lateinit var dir: File

    private lateinit var disk: DiskCache

    @BeforeEach
    fun setUp() {
        disk = realDiskCache(File(dir, "coil"))
    }

    @AfterEach
    fun tearDown() {
        disk.shutdown()
    }

    private fun cacheSnapshot(key: String, text: String) {
        val editor = disk.openEditor(key)!!
        disk.fileSystem.write(editor.data) { writeUtf8(text) }
        editor.commit()
    }

    @Test
    fun libraryEntryNeedsACoverFile() = runTest {
        shouldThrow<IllegalStateException> { coverFetcher { isLibrary = true }.fetch() }
    }

    @Test
    fun libraryCoverFileIsRead() = runTest {
        val cached = File(dir, "cover").apply { writeText("library") }
        val fetcher = coverFetcher {
            isLibrary = true
            coverFile = cached
        }
        fetcher.fetch().sourceResult().text() shouldBe "library"
    }

    @Test
    fun unreadableCoverFileIsRefetched() = runTest {
        val cached = File(dir, "cover").apply { writeText("library") }
        val fetcher = coverFetcher {
            isLibrary = true
            coverFile = cached
            options = coilOptions(disk = CachePolicy.WRITE_ONLY)
        }
        fetcher.fetch().sourceResult().dataSource shouldBe DataSource.DISK
        cached.readText() shouldBe "cover"
    }

    @Test
    fun snapshotIsServed() = runTest {
        cacheSnapshot(key = "key", text = "snap")
        val result = coverFetcher { diskCache = disk }.fetch().sourceResult()
        result.dataSource shouldBe DataSource.DISK
        result.text() shouldBe "snap"
    }

    @Test
    fun snapshotMovesToCoverCache() = runTest {
        cacheSnapshot(key = "key", text = "snap")
        val target = File(dir, "library/cover")
        val fetcher = coverFetcher {
            diskCache = disk
            isLibrary = true
            coverFile = target
        }
        fetcher.fetch().sourceResult().text() shouldBe "snap"
        target.readText() shouldBe "snap"
        disk.openSnapshot("key").shouldBeNull()
    }

    @Test
    fun snapshotIsClosedOnFailure() = runTest {
        cacheSnapshot(key = "key", text = "snap")
        val fetcher = coverFetcher {
            diskCache = disk
            diskCacheKey = FlakyKey()
        }
        shouldThrow<IllegalStateException> { fetcher.fetch() }
        disk.openSnapshot("key")!!.close()
    }

    @Test
    fun missingCacheFileIsNotReturned() {
        cacheSnapshot(key = "key", text = "snap")
        val snapshot = disk.openSnapshot("key")!!
        val noDisk = coverFetcher()
        noDisk.moveSnapshotToCoverCache(snapshot, File(dir, "never")).shouldBeNull()
        noDisk.moveSnapshotToCoverCache(snapshot, null).shouldBeNull()
        snapshot.close()
    }

    @Test
    fun unwritableCacheFileIsSkipped() {
        cacheSnapshot(key = "key", text = "snap")
        val snapshot = disk.openSnapshot("key")!!
        val directory = stubbornDirectory(dir, "a-directory")
        coverFetcher { diskCache = disk }.moveSnapshotToCoverCache(snapshot, directory).shouldBeNull()
        snapshot.close()
    }

    @Test
    fun parentlessFileIsWritten() {
        val relative = File("cover-under-test-${System.nanoTime()}")
        try {
            coverFetcher().writeSourceToCoverCache(okio.Buffer().writeUtf8("x"), relative)
            relative.readText() shouldBe "x"
        } finally {
            relative.delete()
        }
    }
}
