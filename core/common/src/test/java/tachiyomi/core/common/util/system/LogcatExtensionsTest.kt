package tachiyomi.core.common.util.system

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import logcat.LogPriority
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

/**
 * `Any.logcat` is inline, so every call site gets its own copy; the compiled body in
 * `LogcatExtensionsKt` only runs when invoked through reflection, which is what these tests do.
 */
internal class LogcatExtensionsTest {
    private val logger = RecordingLogcatLogger

    private val facade: Class<*> = Class.forName("tachiyomi.core.common.util.system.LogcatExtensionsKt")

    private val logcatMethod: Method = facade.getDeclaredMethod(
        "logcat",
        Any::class.java,
        LogPriority::class.java,
        Throwable::class.java,
        String::class.java,
        Function0::class.java,
    )

    private val logcatDefault: Method = facade.getDeclaredMethod(
        "logcat\$default",
        Any::class.java,
        LogPriority::class.java,
        Throwable::class.java,
        String::class.java,
        Function0::class.java,
        Int::class.javaPrimitiveType,
        Any::class.java,
    )

    private fun log(priority: LogPriority, throwable: Throwable?, tag: String?, message: () -> String) {
        logcatMethod.invoke(null, this, priority, throwable, tag, message)
    }

    @BeforeEach
    fun setUp() {
        logger.start()
    }

    @Test
    fun plainMessageUsesTheCallerTag() {
        log(LogPriority.INFO, null, null) { "hello" }
        logger.entries.map { it.priority } shouldContainExactly listOf(LogPriority.INFO)
        logger.entries.single().tag shouldBe "LogcatExtensionsTest"
        logger.messages() shouldContainExactly listOf("hello")
    }

    @Test
    fun tagIsPrefixedInBrackets() {
        log(LogPriority.DEBUG, null, "Sync") { "started" }
        logger.messages() shouldContainExactly listOf("[Sync] started")
    }

    @Test
    fun emptyTagIsOmitted() {
        log(LogPriority.DEBUG, null, "") { "started" }
        logger.messages() shouldContainExactly listOf("started")
    }

    @Test
    fun throwableFollowsOnNewLine() {
        log(LogPriority.ERROR, IllegalStateException("boom"), null) { "failed" }
        val message = logger.messages().single()
        message shouldStartWith "failed\n"
        message shouldContain "IllegalStateException: boom"
    }

    @Test
    fun throwableAloneHasNoNewLine() {
        log(LogPriority.WARN, IllegalArgumentException("bad"), "T") { " " }
        val message = logger.messages().single()
        message shouldStartWith "[T]  java.lang.IllegalArgumentException: bad"
    }

    @Test
    fun defaultsAreDebugAndBare() {
        logcatDefault.invoke(null, this, null, null, null, null, ALL_DEFAULTS, null)
        val entry = logger.entries.single()
        entry.priority shouldBe LogPriority.DEBUG
        entry.tag shouldBe "LogcatExtensionsTest"
        entry.message shouldBe ""
    }

    @Test
    fun explicitArgsSurviveTheBridge() {
        val message: () -> String = { "kept" }
        logcatDefault.invoke(null, this, LogPriority.VERBOSE, null, "K", message, 0, null)
        logger.entries.single().priority shouldBe LogPriority.VERBOSE
        logger.messages() shouldContainExactly listOf("[K] kept")
    }

    private companion object {
        // Bits 0..3: priority, throwable, tag and message all take their default.
        const val ALL_DEFAULTS = 0b1111
    }
}
