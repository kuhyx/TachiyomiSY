package eu.kanade.tachiyomi.data.coil

import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.source.PagePreviewSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okio.Source
import okio.buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

internal class PagePreviewFetcherTest {

    @TempDir
    lateinit var dir: File

    private lateinit var disk: DiskCache
    private lateinit var previewFile: File
    private var failWrites = false

    @BeforeEach
    fun setUp() {
        disk = realDiskCache(File(dir, "coil"))
        previewFile = File(dir, "preview")
    }

    @AfterEach
    fun tearDown() {
        disk.shutdown()
    }

    private fun fetcher(
        options: coil3.request.Options = coilOptions(),
        key: Lazy<String> = lazyOf("key"),
        source: PagePreviewSource? = null,
        client: Call.Factory = cannedClient(),
        diskCache: DiskCache? = disk,
        cacheAccepts: Boolean = true,
    ) = PagePreviewFetcher(
        page = PagePreview(index = 0, imageUrl = "https://example.org/p.jpg", source = 1L),
        options = options,
        pagePreviewFile = { previewFile },
        isInCache = { cacheAccepts && previewFile.exists() },
        writeToCache = { input: Source ->
            if (failWrites) throw IOException("full")
            previewFile.writeText(input.buffer().readUtf8())
        },
        diskCacheKeyLazy = key,
        sourceLazy = lazyOf(source),
        callFactoryLazy = lazyOf(client),
        imageLoader = loaderWith(diskCache),
    )

    private fun cacheSnapshot(text: String) {
        val editor = disk.openEditor("key")!!
        disk.fileSystem.write(editor.data) { writeUtf8(text) }
        editor.commit()
    }

    @Test
    fun cachedPreviewIsRead() = runTest {
        previewFile.writeText("cached")
        fetcher().fetch().sourceResult().text() shouldBe "cached"
        fetcher(options = coilOptions(disk = CachePolicy.WRITE_ONLY)).fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun networkResponseIsCached() = runTest {
        fetcher().fetch().sourceResult().dataSource shouldBe DataSource.DISK
        previewFile.readText() shouldBe "cover"
    }

    @Test
    fun rejectedWritesUseDiskCache() = runTest {
        fetcher(cacheAccepts = false).fetch().sourceResult().dataSource shouldBe DataSource.NETWORK
        failWrites = true
        fetcher(diskCache = null, cacheAccepts = false).fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun offlineReadsTheHttpCache() = runTest {
        val offline = coilOptions(disk = CachePolicy.READ_ONLY, network = CachePolicy.DISABLED)
        val result = fetcher(
            options = offline,
            client = cannedClient(fromCache = true),
            diskCache = null,
            cacheAccepts = false,
        ).fetch().sourceResult()
        result.dataSource shouldBe DataSource.DISK
    }

    @Test
    fun snapshotMovesToPreviewCache() = runTest {
        cacheSnapshot("snap")
        fetcher().fetch().sourceResult().text() shouldBe "snap"
        previewFile.readText() shouldBe "snap"
    }

    @Test
    fun snapshotServedWhenNotMoved() = runTest {
        cacheSnapshot("snap")
        fetcher(cacheAccepts = false).fetch().sourceResult().text() shouldBe "snap"
        failWrites = true
        fetcher().fetch().sourceResult().text() shouldBe "snap"
    }

    @Test
    fun snapshotWithoutDiskIsNotMoved() {
        cacheSnapshot("snap")
        val snapshot = disk.openSnapshot("key")!!
        fetcher(diskCache = null).moveSnapshotToPagePreviewCache(snapshot).shouldBeNull()
        snapshot.close()
    }

    @Test
    fun failuresCloseWhatWasOpened() = runTest {
        cacheSnapshot("snap")
        shouldThrow<IllegalStateException> { fetcher(key = FlakyKey(), cacheAccepts = false).fetch() }
        disk.remove("key")
        shouldThrow<IllegalStateException> { fetcher(key = FlakyKey(goodReads = 2), cacheAccepts = false).fetch() }
        disk.remove("key")
        shouldThrow<IOException> { fetcher(client = cannedClient(body = BrokenBody()), cacheAccepts = false).fetch() }
        val gone = cannedClient(code = 404)
        shouldThrow<IOException> { fetcher(client = gone, diskCache = null, cacheAccepts = false).fetch() }
        val unchanged = fetcher(client = cannedClient(code = 304), diskCache = null, cacheAccepts = false)
        unchanged.fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun sourceServesItsOwnPreviews() = runTest {
        val source = mockk<PagePreviewSource>()
        coEvery { source.fetchPreviewImage(any(), any()) } returns imageResponse()
        fetcher(source = source, diskCache = null, cacheAccepts = false).fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun diskWriteNeedsAnEditor() {
        fetcher(diskCache = null).writeToDiskCache(imageResponse()).shouldBeNull()
        val busy = disk.openEditor("key")!!
        fetcher().writeToDiskCache(imageResponse()).shouldBeNull()
        busy.abort()
        shouldThrow<IOException> { fetcher().writeToDiskCache(imageResponse(BrokenBody())) }
    }
}
