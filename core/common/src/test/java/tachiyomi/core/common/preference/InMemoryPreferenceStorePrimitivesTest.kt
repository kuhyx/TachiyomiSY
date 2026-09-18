package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val STRING_KEY = "s"
private const val LONG_KEY = "l"
private const val INT_KEY = "i"
private const val FLOAT_KEY = "f"
private const val BOOLEAN_KEY = "b"
private const val SET_KEY = "set"
private const val OTHER_KEY = "other"

private fun populatedStore(): InMemoryPreferenceStore = InMemoryPreferenceStore(
    sequenceOf(
        InMemoryPreferenceStore.InMemoryPreference(STRING_KEY, "stored", "d"),
        InMemoryPreferenceStore.InMemoryPreference(LONG_KEY, 5L, 0L),
        InMemoryPreferenceStore.InMemoryPreference(INT_KEY, 6, 0),
        InMemoryPreferenceStore.InMemoryPreference(FLOAT_KEY, 1.5f, 0f),
        InMemoryPreferenceStore.InMemoryPreference(BOOLEAN_KEY, true, false),
        InMemoryPreferenceStore.InMemoryPreference(SET_KEY, setOf("x"), emptySet<String>()),
        InMemoryPreferenceStore.InMemoryPreference(OTHER_KEY, Any(), Any()),
    ),
)

internal class InMemoryPreferenceStorePrimitivesTest {
    private val store = populatedStore()

    @Test
    fun stringAbsentPresentMismatch() {
        store.getString("missing", "d").get() shouldBe "d"
        store.getString(STRING_KEY, "d").get() shouldBe "stored"
        store.getString(OTHER_KEY, "d").get() shouldBe "d"
        store.getString(LONG_KEY).get() shouldBe ""
    }

    @Test
    fun longAbsentPresentMismatch() {
        store.getLong("missing", 9L).get() shouldBe 9L
        store.getLong(LONG_KEY, 9L).get() shouldBe 5L
        store.getLong(STRING_KEY, 9L).get() shouldBe 9L
        store.getLong(STRING_KEY).get() shouldBe 0L
    }

    @Test
    fun intAbsentPresentMismatch() {
        store.getInt("missing", 9).get() shouldBe 9
        store.getInt(INT_KEY, 9).get() shouldBe 6
        store.getInt(STRING_KEY, 9).get() shouldBe 9
        store.getInt(STRING_KEY).get() shouldBe 0
    }

    @Test
    fun floatAbsentPresentMismatch() {
        store.getFloat("missing", 9f).get() shouldBe 9f
        store.getFloat(FLOAT_KEY, 9f).get() shouldBe 1.5f
        store.getFloat(STRING_KEY, 9f).get() shouldBe 9f
        store.getFloat(STRING_KEY).get() shouldBe 0f
    }

    @Test
    fun booleanAbsentPresentMismatch() {
        store.getBoolean("missing", true).get() shouldBe true
        store.getBoolean(BOOLEAN_KEY, false).get() shouldBe true
        store.getBoolean(STRING_KEY, true).get() shouldBe true
        store.getBoolean(STRING_KEY).get() shouldBe false
    }

    @Test
    fun stringSetAbsentPresentMismatch() {
        store.getStringSet("missing", setOf("d")).get() shouldBe setOf("d")
        store.getStringSet(SET_KEY, setOf("d")).get() shouldBe setOf("x")
        store.getStringSet(STRING_KEY, setOf("d")).get() shouldBe setOf("d")
        store.getStringSet(STRING_KEY).get() shouldBe emptySet()
    }

    @Test
    fun presentValuesReportIsSet() {
        store.getString(STRING_KEY, "d").isSet() shouldBe true
        store.getString("missing", "d").isSet() shouldBe false
        store.getString(STRING_KEY, "d").key() shouldBe STRING_KEY
    }
}
