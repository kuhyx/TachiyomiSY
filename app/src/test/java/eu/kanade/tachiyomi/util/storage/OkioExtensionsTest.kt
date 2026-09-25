package eu.kanade.tachiyomi.util.storage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

internal class OkioExtensionsTest {

    @TempDir
    lateinit var dir: File

    private fun source(text: String): BufferedSource = Buffer().writeUtf8(text)

    @Test
    fun savesToAStream() {
        val output = ByteArrayOutputStream()
        source("hello").saveTo(output)
        output.toString() shouldBe "hello"
    }

    @Test
    fun savesToAFileCreatingParents() {
        val file = File(dir, "nested/deeper/out.txt")
        source("payload").saveTo(file)
        file.readText() shouldBe "payload"
    }

    @Test
    fun aBareFileNameHasNoParentTo() {
        val file = File("okio-extensions-test.tmp")
        try {
            source("bare").saveTo(file)
            file.readText() shouldBe "bare"
        } finally {
            file.delete()
        }
    }

    @Test
    fun aFailedSaveRemovesTheFile() {
        val file = File(dir, "broken.txt")
        val failing = object : InputStream() {
            override fun read(): Int = throw IOException("boom")
        }.source().buffer()
        shouldThrow<IOException> { failing.saveTo(file) }
        file.exists() shouldBe false
    }
}
