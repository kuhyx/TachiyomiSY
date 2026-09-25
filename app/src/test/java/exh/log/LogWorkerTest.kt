package exh.log

import com.elvishew.xlog.flattener.Flattener2
import com.elvishew.xlog.printer.file.backup.BackupStrategy
import com.elvishew.xlog.printer.file.naming.FileNameGenerator
import com.hippo.unifile.UniFile
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class LogWorkerTest {
    private val out = ByteArrayOutputStream()

    private fun printer(): EnhancedFilePrinter {
        val file = mockk<UniFile> {
            every { name } returns "log.txt"
            every { openOutputStream() } returns out
        }
        val folder = mockk<UniFile> {
            every { listFiles() } returns null
            every { createFile(any()) } returns file
        }
        val names = mockk<FileNameGenerator> {
            every { isFileNameChangeable } returns true
            every { generateFileName(any(), any()) } returns "log.txt"
        }
        return EnhancedFilePrinter(folder, names, mockk<BackupStrategy>(), Flattener2 { _, _, tag, msg -> "$tag:$msg" })
    }

    private fun waitFor(what: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!what() && System.currentTimeMillis() < deadline) Thread.sleep(10)
        what().shouldBeTrue()
    }

    @Test
    fun startedWorkerWritesQueuedLogs() {
        val worker = LogWorker(printer())
        worker.isStarted().shouldBeFalse()
        worker.start()
        worker.isStarted().shouldBeTrue()
        worker.enqueue(EnhancedFilePrinter.LogItem(1L, LogLevel.Info.int, "t", "first"))
        worker.enqueue(EnhancedFilePrinter.LogItem(2L, LogLevel.Info.int, "t", "second"))
        waitFor { out.toString() == "t:first\nt:second\n" }
    }

    @Test
    fun interruptedTakeStopsTheWorker() {
        val worker = LogWorker(printer())
        val thread = Thread(worker)
        worker.start()
        thread.start()
        thread.interrupt()
        thread.join(5_000)
        worker.isStarted().shouldBeFalse()
    }

    @Test
    fun interruptedEnqueueIsDropped() {
        val worker = LogWorker(printer())
        Thread.currentThread().interrupt()
        worker.enqueue(EnhancedFilePrinter.LogItem(1L, LogLevel.Info.int, "t", "lost"))
        Thread.interrupted().shouldBeFalse()
        worker.start()
        worker.enqueue(EnhancedFilePrinter.LogItem(1L, LogLevel.Info.int, "t", "kept"))
        waitFor { out.toString() == "t:kept\n" }
        out.toString() shouldBe "t:kept\n"
    }
}
