package exh.log

import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

internal class LogFileWriterTest {
    private fun file(fileName: String, stream: OutputStream): UniFile = mockk {
        every { name } returns fileName
        every { openOutputStream() } returns stream
    }

    @Test
    fun opensAppendsAndCloses() {
        val writer = LogFileWriter()
        writer.isOpened.shouldBeFalse()
        writer.lastFileName.shouldBeNull()
        val out = ByteArrayOutputStream()
        val file = file("a.log", out)
        writer.open(file).shouldBeTrue()
        writer.isOpened.shouldBeTrue()
        writer.lastFileName shouldBe "a.log"
        writer.file shouldBeSameInstanceAs file
        writer.appendLog("hello")
        out.toString() shouldBe "hello\n"
        writer.close().shouldBeTrue()
        writer.isOpened.shouldBeFalse()
        writer.lastFileName.shouldBeNull()
        writer.file.shouldBeNull()
        writer.close().shouldBeTrue()
    }

    @Test
    fun openFailureIsReported() {
        val writer = LogFileWriter()
        val file = mockk<UniFile> { every { openOutputStream() } throws IOException("denied") }
        writer.open(file).shouldBeFalse()
        writer.isOpened.shouldBeFalse()
    }

    @Test
    fun appendRequiresAnOpenFile() {
        shouldThrow<IllegalArgumentException> { LogFileWriter().appendLog("x") }
    }

    @Test
    fun writeAndCloseFailuresContained() {
        val failing = object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun close() = throw IOException("cannot close")
        }
        val writer = LogFileWriter()
        writer.open(file("b.log", failing)).shouldBeTrue()
        writer.appendLog("dropped")
        writer.close().shouldBeFalse()
        writer.isOpened.shouldBeFalse()
        writer.lastFileName.shouldBeNull()
    }
}
