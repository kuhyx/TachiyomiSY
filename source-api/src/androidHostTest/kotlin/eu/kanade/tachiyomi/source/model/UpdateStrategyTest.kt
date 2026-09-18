package eu.kanade.tachiyomi.source.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class UpdateStrategyTest {
    @Test
    fun entriesInDeclarationOrder() {
        UpdateStrategy.entries shouldContainExactly listOf(UpdateStrategy.ALWAYS_UPDATE, UpdateStrategy.ONLY_FETCH_ONCE)
        UpdateStrategy.ALWAYS_UPDATE.ordinal shouldBe 0
        UpdateStrategy.ONLY_FETCH_ONCE.ordinal shouldBe 1
    }

    @Test
    fun valueOfResolvesNames() {
        UpdateStrategy.valueOf("ALWAYS_UPDATE") shouldBe UpdateStrategy.ALWAYS_UPDATE
        UpdateStrategy.valueOf("ONLY_FETCH_ONCE") shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        UpdateStrategy.ONLY_FETCH_ONCE.name shouldBe "ONLY_FETCH_ONCE"
    }

    @Test
    fun valueOfRejectsUnknown() {
        shouldThrow<IllegalArgumentException> { UpdateStrategy.valueOf("NEVER") }
    }
}
