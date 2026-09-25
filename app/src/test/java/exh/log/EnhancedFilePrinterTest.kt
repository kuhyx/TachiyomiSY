package exh.log

import com.elvishew.xlog.flattener.Flattener2
import com.elvishew.xlog.printer.file.backup.BackupStrategy
import com.elvishew.xlog.printer.file.naming.FileNameGenerator
import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class EnhancedFilePrinterTest {
    private val out = ByteArrayOutputStream()
    private val logFile = mockk<UniFile> {
        every { name } returns "log.txt"
        every { openOutputStream() } returns out
    }
    private val folder = mockk<UniFile> {
        every { listFiles() } returns null
        every { createFile(any()) } returns logFile
    }
    private val names = mockk<FileNameGenerator> {
        every { isFileNameChangeable } returns true
        every { generateFileName(any(), any()) } returns "log.txt"
    }
    private val flattener = Flattener2 { _, _, tag, msg -> "$tag:$msg" }

    private fun printer() = EnhancedFilePrinter.Builder(folder)
        .fileNameGenerator(names)
        .backupStrategy(mockk<BackupStrategy>())
        .flattener(flattener)
        .build()

    @Test
    fun builderFillsDefaults() {
        val builder = EnhancedFilePrinter.Builder(folder)
        builder.fileNameGenerator shouldBe null
        builder.backupStrategy shouldBe null
        builder.flattener shouldBe null
        builder.build().shouldNotBeNull()
        val custom = EnhancedFilePrinter.Builder(folder) { fileNameGenerator(names) }
        custom.shouldNotBeNull()
        builder.flattener(flattener) shouldBeSameInstanceAs builder
        builder.flattener shouldBeSameInstanceAs flattener
    }

    @Test
    fun printlnGoesThroughTheWorker() {
        val printer = printer()
        printer.println(LogLevel.Info.int, "t", "one")
        printer.println(LogLevel.Info.int, "t", "two")
        val deadline = System.currentTimeMillis() + 5_000
        while (out.toString() != "t:one\nt:two\n" && System.currentTimeMillis() < deadline) Thread.sleep(10)
        out.toString() shouldBe "t:one\nt:two\n"
    }

    @Test
    fun doPrintlnOpensFilesOnDemand() {
        val printer = printer()
        printer.doPrintln(1L, LogLevel.Info.int, "t", "a")
        printer.doPrintln(2L, LogLevel.Info.int, "t", "b")
        verify(exactly = 1) { folder.createFile("log.txt") }
        out.toString() shouldBe "t:a\nt:b\n"
        // A new file name closes the old file and cleans the folder of week-old logs.
        val old = mockk<UniFile>(relaxed = true) { every { lastModified() } returns 0L }
        val fresh = mockk<UniFile>(relaxed = true) { every { lastModified() } returns System.currentTimeMillis() }
        every { folder.listFiles() } returns arrayOf(old, fresh)
        every { names.generateFileName(any(), any()) } returns "next.txt"
        every { folder.createFile("next.txt") } returns logFile
        printer.doPrintln(3L, LogLevel.Info.int, "t", "c")
        verify(exactly = 1) { old.delete() }
        verify(exactly = 0) { fresh.delete() }
    }

    @Test
    fun fixedFileNamesNeverReopen() {
        every { names.isFileNameChangeable } returns false
        val printer = printer()
        shouldThrow<IllegalArgumentException> { printer.doPrintln(1L, LogLevel.Info.int, "t", "a") }
    }

    @Test
    fun blankFileNamesAreRejected() {
        val printer = printer()
        every { names.generateFileName(any(), any()) } returns " "
        shouldThrow<IllegalArgumentException> { printer.doPrintln(1L, LogLevel.Info.int, "t", "a") }
        every { names.generateFileName(any(), any()) } returns null
        shouldThrow<IllegalArgumentException> { printer.doPrintln(1L, LogLevel.Info.int, "t", "a") }
    }

    @Test
    fun unwritableFilesAreSkipped() {
        val printer = printer()
        every { folder.createFile(any()) } returns null
        printer.doPrintln(1L, LogLevel.Info.int, "t", "a")
        every { folder.createFile(any()) } returns mockk { every { openOutputStream() } throws java.io.IOException() }
        printer.doPrintln(1L, LogLevel.Info.int, "t", "b")
        out.size() shouldBe 0
    }

    @Test
    fun logItemIsAPlainValue() {
        val item = EnhancedFilePrinter.LogItem(1L, 2, "t", "m")
        item.copy(msg = "n").msg shouldBe "n"
        (item == EnhancedFilePrinter.LogItem(1L, 2, "t", "m")).shouldBeTrue()
        item.toString() shouldBe "LogItem(timeMillis=1, level=2, tag=t, msg=m)"
    }
}
