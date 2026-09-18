package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

private const val OBJECT_KEY = "o"
private const val SET_KEY = "set"

private fun objectStore(): InMemoryPreferenceStore = InMemoryPreferenceStore(
    sequenceOf(
        InMemoryPreferenceStore.InMemoryPreference(OBJECT_KEY, 7, 0),
        InMemoryPreferenceStore.InMemoryPreference(SET_KEY, setOf(1, 2), emptySet<Int>()),
    ),
)

internal class InMemoryPreferenceStoreObjectsTest {
    private val store = objectStore()

    @Test
    fun objectFromStringAbsentPresent() {
        val absent = store.getObjectFromString(
            key = "missing",
            defaultValue = 1,
            serializer = { it.toString() },
            deserializer = { it.toInt() },
        )
        absent.get() shouldBe 1
        val present = store.getObjectFromString(
            key = OBJECT_KEY,
            defaultValue = 1,
            serializer = { it.toString() },
            deserializer = { it.toInt() },
        )
        present.get() shouldBe 7
    }

    @Test
    fun objectFromIntAbsentPresent() {
        val absent = store.getObjectFromInt(
            key = "missing",
            defaultValue = 1,
            serializer = { it },
            deserializer = { it },
        )
        absent.get() shouldBe 1
        val present = store.getObjectFromInt(
            key = OBJECT_KEY,
            defaultValue = 1,
            serializer = { it },
            deserializer = { it },
        )
        present.get() shouldBe 7
    }

    @Test
    fun objectSetAbsentPresent() {
        val absent = store.getObjectSetFromStringSet(
            key = "missing",
            defaultValue = setOf(9),
            serializer = { it.toString() },
            deserializer = { it.toIntOrNull() },
        )
        absent.get() shouldBe setOf(9)
        val present = store.getObjectSetFromStringSet(
            key = SET_KEY,
            defaultValue = setOf(9),
            serializer = { it.toString() },
            deserializer = { it.toIntOrNull() },
        )
        present.get() shouldBe setOf(1, 2)
    }

    @Test
    fun getAllExposesInitialPrefs() {
        store.getAll().keys shouldBe setOf(OBJECT_KEY, SET_KEY)
        InMemoryPreferenceStore().getAll() shouldBe emptyMap<String, Any>()
    }

    @Test
    fun prefReadsWritesDeletes() {
        val pref = InMemoryPreferenceStore.InMemoryPreference("k", null, "d")
        pref.key() shouldBe "k"
        pref.isSet() shouldBe false
        pref.get() shouldBe "d"
        pref.defaultValue() shouldBe "d"
        pref.set("v")
        pref.isSet() shouldBe true
        pref.get() shouldBe "v"
        pref.delete()
        pref.isSet() shouldBe false
        pref.get() shouldBe "d"
    }

    @Test
    fun prefChangesEmitNothing() {
        val pref = InMemoryPreferenceStore.InMemoryPreference("k", "v", "d")
        runBlocking { pref.changes().toList() } shouldBe emptyList()
    }

    @Test
    fun prefStateStartsAtCurrent() {
        val pref = InMemoryPreferenceStore.InMemoryPreference("k", "v", "d")
        runBlocking { pref.stateIn(this).value } shouldBe "v"
    }
}
