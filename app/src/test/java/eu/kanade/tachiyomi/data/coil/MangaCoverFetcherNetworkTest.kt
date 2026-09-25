package eu.kanade.tachiyomi.data.coil

import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.ForwardingSource
import okio.buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

/** A body whose bytes cannot be read: the disk-cache write fails on it. */
internal class BrokenBody : ResponseBody() {
    override fun contentType(): MediaType? = null

    override fun contentLength(): Long = -1

    override fun source(): BufferedSource = object : ForwardingSource(Buffer()) {
        override fun read(sink: Buffer, byteCount: Long): Long = throw IOException("reset")
    }.buffer()
}

internal class MangaCoverFetcherNetworkTest {

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

    @Test
    fun responseIsStoredInDiskCache() = runTest {
        val result = coverFetcher { diskCache = disk }.fetch().sourceResult()
        result.dataSource shouldBe DataSource.NETWORK
        result.text() shouldBe "cover"
        disk.openSnapshot("key")!!.close()
    }

    @Test
    fun cachedResponseCountsAsDisk() = runTest {
        coverFetcher { client = cannedClient(fromCache = true) }.fetch().sourceResult().dataSource shouldBe
            DataSource.DISK
    }

    @Test
    fun errorResponsesThrow() = runTest {
        shouldThrow<IOException> { coverFetcher { client = cannedClient(code = 500) }.fetch() }
        coverFetcher { client = cannedClient(code = 304) }.fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun sourceClientAndHeaders() = runTest {
        var seen = ""
        val source = mockk<HttpSource>()
        every { source.headers } returns headersOf("Referer", "https://source/")
        every { source.client } returns cannedClient(onRequest = { seen = it.header("Referer").orEmpty() })
        coverFetcher { this.source = source }.fetch()
        seen shouldBe "https://source/"
    }

    @Test
    fun offlineRequestsOnlyHitTheCache() {
        coverFetcher { options = coilOptions(network = CachePolicy.DISABLED) }.newRequest().cacheControl
            .onlyIfCached shouldBe true
        coverFetcher().newRequest().cacheControl.noStore shouldBe true
    }

    @Test
    fun disabledWritesSkipCoverCache() {
        val fetcher = coverFetcher { options = coilOptions(disk = CachePolicy.READ_ONLY) }
        fetcher.writeResponseToCoverCache(imageResponse(), File(dir, "c")).shouldBeNull()
        coverFetcher().writeResponseToCoverCache(imageResponse(), null).shouldBeNull()
        coverFetcher().writeResponseToCoverCache(imageResponse(), stubbornDirectory(dir, "d")).shouldBeNull()
    }

    @Test
    fun diskWriteNeedsAnEditor() {
        coverFetcher().writeToDiskCache(imageResponse()).shouldBeNull()
        val busy = disk.openEditor("key")!!
        coverFetcher { diskCache = disk }.writeToDiskCache(imageResponse()).shouldBeNull()
        busy.abort()
    }

    @Test
    fun failedDiskWriteAborts() = runTest {
        shouldThrow<IOException> { coverFetcher { diskCache = disk }.writeToDiskCache(imageResponse(BrokenBody())) }
        shouldThrow<IOException> {
            coverFetcher {
                diskCache = disk
                client = cannedClient(body = BrokenBody())
            }.fetch()
        }
    }

    @Test
    fun failedAbortIsIgnored() {
        val editor = mockk<DiskCache.Editor>()
        every { editor.data } throws IOException("gone")
        every { editor.abort() } throws IOException("also gone")
        val stubbed = mockk<DiskCache>()
        every { stubbed.openEditor("key") } returns editor
        every { stubbed.fileSystem } returns FileSystem.SYSTEM
        shouldThrow<IOException> { coverFetcher { diskCache = stubbed }.writeToDiskCache(imageResponse()) }
    }

    @Test
    fun snapshotFailureClosesIt() = runTest {
        val fetcher = coverFetcher {
            diskCache = disk
            diskCacheKey = FlakyKey(goodReads = 2)
        }
        shouldThrow<IllegalStateException> { fetcher.fetch() }
    }
}
