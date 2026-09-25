package eu.kanade.domain

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.preference.getEnumSet
import tachiyomi.core.common.preference.getLongArray

/** The test double itself: the guarantees the interactor tests build on. */
internal class FlowPreferenceStoreTest {

    private enum class Mode { A, B }

    private val store = FlowPreferenceStore()

    @Test
    fun valuesPersistAcrossLookups() = runTest {
        store.getString("s").set("x")
        store.getString("s").get() shouldBe "x"
        store.getLong("l", 1).get() shouldBe 1L
        store.getLong("l", 1).set(2)
        store.getInt("i").set(3)
        store.getFloat("f").set(4f)
        store.getBoolean("b").set(true)
        store.getStringSet("set").set(setOf("a"))
        store.getAll() shouldBe mapOf("s" to "x", "l" to 2L, "i" to 3, "f" to 4f, "b" to true, "set" to setOf("a"))
        store.getString("s").changes().first() shouldBe "x"
        store.getString("s").stateIn(backgroundScope).value shouldBe "x"
        store.getString("s").isSet() shouldBe true
        store.getString("s").delete()
        store.getString("s").isSet() shouldBe false
        store.getString("s").defaultValue() shouldBe ""
        store.getString("s").key() shouldBe "s"
    }

    @Test
    fun objectsRoundTripSerialized() {
        store.getEnum("m", Mode.A).get() shouldBe Mode.A
        store.getEnum("m", Mode.A).set(Mode.B)
        store.getEnum("m", Mode.A).get() shouldBe Mode.B
        store.getString("m").get() shouldBe "B"
        store.getLongArray("ids", emptyList()).set(listOf(1L, 2L))
        store.getLongArray("ids", emptyList()).get() shouldBe listOf(1L, 2L)
        store.getEnumSet("modes", emptySet<Mode>()).set(setOf(Mode.B))
        store.getEnumSet("modes", emptySet<Mode>()).get() shouldBe setOf(Mode.B)
        store.getObjectFromInt("n", 0, { it * 2 }, { it / 2 }).set(21)
        store.getInt("n").get() shouldBe 42
        store.getObjectFromInt("n", 0, { it * 2 }, { it / 2 }).get() shouldBe 21
    }
}
