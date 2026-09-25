package exh.log

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import io.kotest.matchers.collections.shouldContainExactly
import logcat.LogPriority
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class XLogLogcatLoggerTest {
    private val printed = mutableListOf<Triple<Int, String?, String?>>()

    @BeforeEach
    fun captureXLog() {
        // XLog only takes the first init; the printers are what we can observe.
        XLog.init(
            LogConfiguration.Builder().logLevel(com.elvishew.xlog.LogLevel.ALL).build(),
            Printer { level, tag, msg -> printed += Triple(level, tag, msg) },
        )
    }

    @Test
    fun prioritiesMapToXLogLevels() {
        val logger = XLogLogcatLogger()
        LogPriority.entries.forEach { logger.log(it, "tag-${it.name}", "msg-${it.name}") }
        printed.filter { it.second?.startsWith("tag-") == true }.map { it.first to it.second } shouldContainExactly
            listOf(
                LogLevel.Verbose.int to "tag-VERBOSE",
                LogLevel.Debug.int to "tag-DEBUG",
                LogLevel.Info.int to "tag-INFO",
                LogLevel.Warn.int to "tag-WARN",
                LogLevel.Error.int to "tag-ERROR",
                LogLevel.None.int to "tag-ASSERT",
            )
    }
}
