package exh.util

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class ExceptionUtilTest {
    @Test
    fun ignoreReturnsValueOrNull() {
        ignore { 5 } shouldBe 5
        ignore<Int> { error("boom") }.shouldBeNull()
    }

    /** The non-inlined copy of [ignore] only runs when called reflectively. */
    @Test
    fun ignoreNonInlinedCopy() {
        val method = Class.forName("exh.util.ExceptionUtilKt").getDeclaredMethod("ignore", Function0::class.java)
        method.isAccessible = true
        val seven: () -> Int = { 7 }
        val boom: () -> Int = { error("boom") }
        method.invoke(null, seven) shouldBe 7
        method.invoke(null, boom).shouldBeNull()
    }

    @Test
    fun withRootCauseSetsMissingCause() {
        val root = IllegalStateException("root")
        val top = RuntimeException("top")
        top.withRootCause(root) shouldBeSameInstanceAs top
        top.cause shouldBeSameInstanceAs root
    }

    @Test
    fun withRootCauseWalksChain() {
        val root = IllegalStateException("root")
        val middle = IllegalArgumentException("middle")
        val top = RuntimeException("top", middle)
        top.withRootCause(root)
        top.cause shouldBeSameInstanceAs middle
        middle.cause shouldBeSameInstanceAs root
    }
}
