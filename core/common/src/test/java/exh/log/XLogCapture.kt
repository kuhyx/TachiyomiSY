package exh.log

import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.LogLevel as XLogLevel

/** One line XLog printed: its level, tag and fully formatted message. */
internal data class LogLine(val level: Int, val tag: String, val message: String)

/** Keeps every line XLog prints so a test can assert on what was logged. */
internal class RecordingPrinter {
    val lines = mutableListOf<LogLine>()

    /** The XLog sink that records into [lines]. */
    val printer: Printer = Printer { logLevel, tag, msg -> lines += LogLine(logLevel, tag, msg) }

    fun single(): LogLine = lines.single()
}

/**
 * Re-points the global XLog at a fresh [RecordingPrinter].
 *
 * The JSON and XML formatters are replaced because XLog's defaults need `org.json`, which is a
 * stub outside Robolectric; borders are off so messages come through verbatim.
 */
internal fun installRecordingXLog(): RecordingPrinter {
    val printer = RecordingPrinter()
    val config = LogConfiguration.Builder()
        .logLevel(XLogLevel.ALL)
        .disableBorder()
        .jsonFormatter { "json:$it" }
        .xmlFormatter { "xml:$it" }
        .build()
    XLog.init(config, printer.printer)
    return printer
}
