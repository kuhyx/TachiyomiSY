package eu.kanade.tachiyomi.util.lang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.Closeable
import java.lang.reflect.InvocationTargetException

internal class CloseableExtensionsTest {

    private class Resource(private val failClose: Boolean = false) : Closeable {
        var closed = false

        override fun close() {
            closed = true
            if (failClose) error("close failed")
        }
    }

    @Test
    fun closesEveryResourceAfterThe() {
        val resources = arrayOf(Resource(), null, Resource())
        var ran = false
        resources.use { ran = true }
        ran shouldBe true
        resources.filterNotNull().all { it.closed } shouldBe true
    }

    @Test
    fun blockFailureWinsOverClose() {
        val resources = arrayOf(Resource(failClose = true))
        val failure = shouldThrow<IllegalArgumentException> {
            resources.use { throw IllegalArgumentException("block failed") }
        }
        failure.suppressed.single().message shouldBe "close failed"
        resources[0].closed shouldBe true
    }

    // The inline body only runs when called as a plain method, which reflection can do.
    @Test
    fun compiledInlineBodyCloses() {
        val method = Class.forName("eu.kanade.tachiyomi.util.lang.CloseableExtensionsKt")
            .getDeclaredMethod("use", Array<Closeable>::class.java, Function0::class.java)
        val resources = arrayOf(Resource())
        method.invoke(null, resources, { })
        resources[0].closed shouldBe true
        val failing = arrayOf(Resource(failClose = true))
        val block: () -> Unit = { throw IllegalArgumentException("block failed") }
        val failure = shouldThrow<InvocationTargetException> { method.invoke(null, failing, block) }
        failure.cause!!.suppressed.single().message shouldBe "close failed"
    }

    @Test
    fun closeFailureAloneIsThrown() {
        val resources = arrayOf(Resource(), Resource(failClose = true))
        shouldThrow<IllegalStateException> { resources.use { } }
        resources[0].closed shouldBe true
        shouldThrow<IllegalStateException> { resources.closeAll(null) }
    }
}
