package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import org.junit.jupiter.api.Test

internal class BufferedSourceFetcherTest {

    private val options: Options = Options(
        context = mockk<Context>(relaxed = true),
        fileSystem = FileSystem.SYSTEM,
    )

    @Test
    fun fetchWrapsTheSourceInMemory() = runTest {
        val buffer = Buffer().writeUtf8("image-bytes")
        val result = BufferedSourceFetcher(buffer, options).fetch()
        result.shouldBeSourceResult().dataSource shouldBe DataSource.MEMORY
    }

    @Test
    fun fetchReportsNoMimeType() = runTest {
        val result = BufferedSourceFetcher(Buffer().writeUtf8("x"), options).fetch()
        result.shouldBeSourceResult().mimeType.shouldBeNull()
    }

    @Test
    fun fetchKeepsTheBytesReadable() = runTest {
        val result = BufferedSourceFetcher(Buffer().writeUtf8("hello"), options).fetch()
        result.shouldBeSourceResult().source.source().readUtf8() shouldBe "hello"
    }

    @Test
    fun factoryBuildsAFetcherPerSource() = runTest {
        val buffer = Buffer().writeUtf8("factory")
        val fetcher = BufferedSourceFetcher.Factory().create(
            data = buffer,
            options = options,
            imageLoader = mockk<ImageLoader>(),
        )
        fetcher.fetch()!!.shouldBeSourceResult().source.source().readUtf8() shouldBe "factory"
    }
}

private fun coil3.fetch.FetchResult.shouldBeSourceResult(): SourceFetchResult {
    (this is SourceFetchResult) shouldBe true
    return this as SourceFetchResult
}
