package exh.log

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.elvishew.xlog.LogLevel as XLogLevel

internal class LoggingMessagesTest {
    private lateinit var printer: RecordingPrinter

    @BeforeEach
    fun setUp() {
        printer = installRecordingXLog()
    }

    private fun expect(level: Int, message: String) {
        printer.single() shouldBe LogLine(level, "LoggingMessagesTest", message)
    }

    @Test
    fun errorMessage() {
        xLogE("e")
        expect(XLogLevel.ERROR, "e")
    }

    @Test
    fun warnMessage() {
        xLogW("w")
        expect(XLogLevel.WARN, "w")
    }

    @Test
    fun debugMessage() {
        xLogD("d")
        expect(XLogLevel.DEBUG, "d")
    }

    @Test
    fun infoMessage() {
        xLogI("i")
        expect(XLogLevel.INFO, "i")
    }

    @Test
    fun leveledMessage() {
        xLog(LogLevel.Verbose, "v")
        expect(XLogLevel.VERBOSE, "v")
    }

    @Test
    fun jsonMessage() {
        xLogJson("""{"a":1}""")
        expect(XLogLevel.DEBUG, """json:{"a":1}""")
    }

    @Test
    fun xmlMessage() {
        xLogXML("<a/>")
        expect(XLogLevel.DEBUG, "xml:<a/>")
    }
}
