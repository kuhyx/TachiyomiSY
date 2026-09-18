package eu.kanade.tachiyomi.network

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.Test

internal class ProgressResponseBodyTest {
    private val listener = RecordingListener()
    private val plain = "text/plain".toMediaType()

    @Test
    fun delegatesTypeAndLength() {
        val body = ProgressResponseBody("hello".toByteArray().toResponseBody(plain), listener, 0L)
        body.contentType() shouldBe plain
        body.contentLength() shouldBe 5L
        body.source() shouldBeSameInstanceAs body.source()
    }

    @Test
    fun reportsProgressAfterExisting() {
        val body = ProgressResponseBody("hello".toResponseBody(plain), listener, 10L)
        val sink = Buffer()
        val source = body.source()
        source.read(sink, 2L) shouldBe 2L
        source.read(sink, 100L) shouldBe 3L
        source.read(sink, 100L) shouldBe -1L
        sink.readUtf8() shouldBe "hello"
        // The buffered source pulls the whole five bytes on the first read and only then sees exhaustion.
        listener.updates shouldContainExactly listOf(
            ProgressUpdate(bytesRead = 15L, contentLength = 5L, done = false),
            ProgressUpdate(bytesRead = 15L, contentLength = 5L, done = true),
        )
    }

    @Test
    fun unknownLengthIsPassedThrough() {
        val body = ProgressResponseBody(ClosableBody("ab"), listener, 0L)
        body.contentLength() shouldBe -1L
        body.contentType() shouldBe plain
        body.string() shouldBe "ab"
        listener.updates.last() shouldBe ProgressUpdate(bytesRead = 2L, contentLength = -1L, done = true)
    }
}
