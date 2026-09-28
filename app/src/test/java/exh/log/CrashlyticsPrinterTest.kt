package exh.log

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class CrashlyticsPrinterTest {
    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun logsAtOrAboveTheThreshold() {
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        mockkStatic("com.google.firebase.crashlytics.FirebaseCrashlyticsKt")
        every { Firebase.crashlytics } returns crashlytics
        val printer = CrashlyticsPrinter(LogLevel.Warn.int)
        printer.println(LogLevel.Debug.int, "t", "quiet")
        printer.println(LogLevel.Warn.int, "t", "warned")
        printer.println(LogLevel.Error.int, null, null)
        verify(exactly = 1) { crashlytics.log("${LogLevel.Warn.int}/t: warned") }
        verify(exactly = 1) { crashlytics.log("${LogLevel.Error.int}/null: null") }
        verify(exactly = 0) { crashlytics.log(match { it.endsWith("quiet") }) }
    }

    /** Without a Firebase app the call fails, and the debug build rethrows the failure. */
    @Test
    fun rethrowsFailuresInDebug() {
        val printer = CrashlyticsPrinter(LogLevel.Warn.int)
        shouldThrow<RuntimeException> { printer.println(LogLevel.Error.int, "t", "m") }
    }

    @Test
    fun releaseBuildsSwallowFailures() {
        crashOnDebug(IllegalStateException("ignored"), isDebug = false)
        shouldThrow<IllegalStateException> { crashOnDebug(IllegalStateException("thrown"), isDebug = true) }
    }

    /** A release build logs nothing more when Crashlytics fails; the printer carries on. */
    @Test
    fun failureIsHandedOn() {
        mockkStatic("exh.log.CrashlyticsPrinterKt")
        every { crashOnDebug(any(), any()) } just Runs
        CrashlyticsPrinter(LogLevel.Warn.int).println(LogLevel.Error.int, "t", "m")
        verify(exactly = 1) { crashOnDebug(any(), any()) }
    }
}
