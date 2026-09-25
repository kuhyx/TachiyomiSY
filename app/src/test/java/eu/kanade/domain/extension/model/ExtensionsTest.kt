package eu.kanade.domain.extension.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ExtensionsTest {

    @Test
    fun isAValue() {
        val extensions = Extensions(
            updates = emptyList(),
            installed = emptyList(),
            available = emptyList(),
            untrusted = emptyList(),
        )
        extensions shouldBe extensions.copy()
        extensions.hashCode() shouldBe extensions.copy().hashCode()
        extensions.toString() shouldBe "Extensions(updates=[], installed=[], available=[], untrusted=[])"
        extensions.component1() shouldBe emptyList()
        extensions.component4() shouldBe emptyList()
    }
}
