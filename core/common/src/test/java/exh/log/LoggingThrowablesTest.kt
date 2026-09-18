package exh.log

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.callStatic
import tachiyomi.core.common.preference.staticMethod
import com.elvishew.xlog.LogLevel as XLogLevel

private const val FACADE = "exh.log.LoggingThrowablesKt"

internal class LoggingThrowablesTest {
    private lateinit var printer: RecordingPrinter
    private val error = IllegalStateException("kaboom")

    @BeforeEach
    fun setUp() {
        printer = installRecordingXLog()
    }

    private fun expect(level: Int, prefix: String) {
        val line = printer.single()
        line.level shouldBe level
        line.tag shouldBe "LoggingThrowablesTest"
        line.message shouldContain prefix
        line.message shouldContain "IllegalStateException: kaboom"
    }

    // The overloads taking only a Throwable are deprecated; a direct call is a compile error under
    // warnings-as-errors, so they are invoked through their compiled static methods.
    private fun callDeprecated(name: String) {
        staticMethod(FACADE, name, listOf(Any::class.java, Throwable::class.java)).callStatic(listOf(this, error))
    }

    @Test
    fun errorWithThrowable() {
        xLogE("e", error)
        expect(XLogLevel.ERROR, "e")
    }

    @Test
    fun warnWithThrowable() {
        xLogW("w", error)
        expect(XLogLevel.WARN, "w")
    }

    @Test
    fun debugWithThrowable() {
        xLogD("d", error)
        expect(XLogLevel.DEBUG, "d")
    }

    @Test
    fun infoWithThrowable() {
        xLogI("i", error)
        expect(XLogLevel.INFO, "i")
    }

    @Test
    fun leveledWithThrowable() {
        xLog(LogLevel.Verbose, "v", error)
        expect(XLogLevel.VERBOSE, "v")
    }

    @Test
    fun deprecatedErrorThrowable() {
        callDeprecated("xLogE")
        expect(XLogLevel.ERROR, "IllegalStateException")
    }

    @Test
    fun deprecatedWarnThrowable() {
        callDeprecated("xLogW")
        expect(XLogLevel.WARN, "IllegalStateException")
    }

    @Test
    fun deprecatedDebugThrowable() {
        callDeprecated("xLogD")
        expect(XLogLevel.DEBUG, "IllegalStateException")
    }

    @Test
    fun deprecatedInfoThrowable() {
        callDeprecated("xLogI")
        expect(XLogLevel.INFO, "IllegalStateException")
    }

    @Test
    fun deprecatedLeveledThrowable() {
        val types = listOf(Any::class.java, LogLevel::class.java, Throwable::class.java)
        staticMethod(FACADE, "xLog", types).callStatic(listOf(this, LogLevel.Info, error))
        expect(XLogLevel.INFO, "IllegalStateException")
        printer.single().message shouldEndWith "java.lang.IllegalStateException: kaboom"
    }
}
