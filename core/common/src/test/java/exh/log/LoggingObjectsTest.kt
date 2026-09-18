package exh.log

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.elvishew.xlog.LogLevel as XLogLevel

private const val TAG = "LoggingObjectsTest"

internal class LoggingObjectsTest {
    private lateinit var printer: RecordingPrinter
    private val nothing: Any? = null

    @BeforeEach
    fun setUp() {
        printer = installRecordingXLog()
    }

    private fun expect(level: Int, vararg messages: String) {
        printer.lines shouldBe messages.map { LogLine(level, TAG, it) }
    }

    @Test
    fun errorObjectAndNull() {
        xLogE(42)
        xLogE(nothing)
        expect(XLogLevel.ERROR, "42", "null")
    }

    @Test
    fun warnObjectAndNull() {
        xLogW(42)
        xLogW(nothing)
        expect(XLogLevel.WARN, "42", "null")
    }

    @Test
    fun debugObjectAndNull() {
        xLogD(42)
        xLogD(nothing)
        expect(XLogLevel.DEBUG, "42", "null")
    }

    @Test
    fun infoObjectAndNull() {
        xLogI(42)
        xLogI(nothing)
        expect(XLogLevel.INFO, "42", "null")
    }

    @Test
    fun leveledObjectAndNull() {
        xLog(LogLevel.Verbose, 42)
        xLog(LogLevel.Verbose, nothing)
        expect(XLogLevel.VERBOSE, "42", "null")
    }

    @Test
    fun errorFormat() {
        xLogE("%s-%d", "a", 1)
        expect(XLogLevel.ERROR, "a-1")
    }

    @Test
    fun warnFormat() {
        xLogW("%s-%d", "a", 1)
        expect(XLogLevel.WARN, "a-1")
    }

    @Test
    fun debugFormat() {
        xLogD("%s-%d", "a", 1)
        expect(XLogLevel.DEBUG, "a-1")
    }

    @Test
    fun infoFormat() {
        xLogI("%s-%d", "a", 1)
        expect(XLogLevel.INFO, "a-1")
    }

    @Test
    fun leveledFormat() {
        xLog(LogLevel.Info, "%s-%d", "a", 1)
        expect(XLogLevel.INFO, "a-1")
    }
}
