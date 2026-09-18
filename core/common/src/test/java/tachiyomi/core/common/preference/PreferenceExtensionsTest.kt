package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val FACADE = "tachiyomi.core.common.preference.PreferenceKt"

internal class PreferenceExtensionsTest {
    private fun <T> memory(value: T): Preference<T> = InMemoryPreferenceStore.InMemoryPreference("k", value, value)

    @Test
    fun privateKeysArePrefixed() {
        Preference.privateKey("x") shouldBe "__PRIVATE_x"
        Preference.isPrivate("__PRIVATE_x") shouldBe true
        Preference.isPrivate("x") shouldBe false
    }

    @Test
    fun appStateKeysArePrefixed() {
        Preference.appStateKey("x") shouldBe "__APP_STATE_x"
        Preference.isAppState("__APP_STATE_x") shouldBe true
        Preference.isAppState("__PRIVATE_x") shouldBe false
    }

    @Test
    fun plusAssignAddsOneItem() {
        val pref = memory(setOf("a"))
        pref += "b"
        pref.get() shouldBe setOf("a", "b")
    }

    @Test
    fun plusAssignAddsManyItems() {
        val pref = memory(setOf("a"))
        pref += listOf("b", "c")
        pref.get() shouldBe setOf("a", "b", "c")
    }

    @Test
    fun minusAssignRemovesOneItem() {
        val pref = memory(setOf("a", "b"))
        pref -= "a"
        pref.get() shouldBe setOf("b")
    }

    @Test
    fun toggleFlipsBothWays() {
        val pref = memory(false)
        pref.toggle() shouldBe true
        pref.get() shouldBe true
        pref.toggle() shouldBe false
        pref.get() shouldBe false
    }

    @Test
    fun getAndSetStoresBlockResult() {
        val pref = memory(1)
        pref.getAndSet { it + 1 }
        pref.get() shouldBe 2
    }

    @Test
    fun getAndSetNonInlinedCopy() {
        val pref = memory(5)
        val increment: (Int) -> Int = { it + 1 }
        staticMethod(FACADE, "getAndSet", listOf(Preference::class.java, Function1::class.java))
            .callStatic(listOf(pref, increment))
        pref.get() shouldBe 6
    }
}
