package exh.log

import android.util.Log
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.elvishew.xlog.LogLevel as XLogLevel

internal class LoggingTest {
    private lateinit var printer: RecordingPrinter

    @BeforeEach
    fun setUp() {
        printer = installRecordingXLog()
    }

    @Test
    fun xLogTagsWithClassName() {
        xLog().d("hello")
        printer.single() shouldBe LogLine(XLogLevel.DEBUG, "LoggingTest", "hello")
    }

    @Test
    fun xLogStackAppendsStackTrace() {
        xLogStack().d("hello")
        val line = printer.single()
        line.tag shouldBe "LoggingTest"
        line.message shouldEndWith "hello"
        line.message shouldContain "LoggingTest.xLogStackAppendsStackTrace"
    }

    @Test
    fun levelsMapToXLogAndAndroid() {
        LogLevel.None.int shouldBe XLogLevel.NONE
        LogLevel.None.androidLevel shouldBe Log.ASSERT
        LogLevel.Error.int shouldBe XLogLevel.ERROR
        LogLevel.Error.androidLevel shouldBe Log.ERROR
        LogLevel.Warn.int shouldBe XLogLevel.WARN
        LogLevel.Warn.androidLevel shouldBe Log.WARN
        LogLevel.Info.int shouldBe XLogLevel.INFO
        LogLevel.Info.androidLevel shouldBe Log.INFO
        LogLevel.Debug.int shouldBe XLogLevel.DEBUG
        LogLevel.Debug.androidLevel shouldBe Log.DEBUG
        LogLevel.Verbose.int shouldBe XLogLevel.VERBOSE
        LogLevel.Verbose.androidLevel shouldBe Log.VERBOSE
        LogLevel.All.int shouldBe XLogLevel.ALL
        LogLevel.All.androidLevel shouldBe Log.VERBOSE
    }

    @Test
    fun namesComeFromXLog() {
        LogLevel.Error.name shouldBe "ERROR"
        LogLevel.Error.shortName shouldBe "E"
        LogLevel.Verbose.name shouldBe "VERBOSE"
        LogLevel.Verbose.shortName shouldBe "V"
        LogLevel.getLevelName(LogLevel.Warn) shouldBe "WARN"
        LogLevel.getLevelShortName(LogLevel.Info) shouldBe "I"
        LogLevel.None.name shouldStartWith "ERROR+"
        LogLevel.All.shortName shouldStartWith "V-"
    }

    @Test
    fun valuesListsEveryLevel() {
        LogLevel.values() shouldContainExactly listOf(
            LogLevel.None,
            LogLevel.Error,
            LogLevel.Warn,
            LogLevel.Info,
            LogLevel.Debug,
            LogLevel.Verbose,
            LogLevel.All,
        )
    }
}
