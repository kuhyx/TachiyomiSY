package tachiyomi.core.common.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val KEY = "k"
private const val SETTLE_YIELDS = 20
private const val TIMEOUT_MS = 5_000L

@RunWith(RobolectricTestRunner::class)
internal class AndroidPreferenceStoreTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var store: AndroidPreferenceStore

    @Before
    fun setUp() {
        prefs = robolectricPrefs()
        store = AndroidPreferenceStore(RuntimeEnvironment.getApplication(), prefs)
    }

    @Test
    fun usesDefaultSharedPrefs() {
        val app = RuntimeEnvironment.getApplication()
        PreferenceManager.getDefaultSharedPreferences(app).edit { putInt(KEY, 3) }
        AndroidPreferenceStore(app).getInt(KEY, 0).get() shouldBe 3
    }

    @Test
    fun primitiveFactories() {
        store.getString(KEY, "d").shouldBeInstanceOf<AndroidPreference.StringPrimitive>().get() shouldBe "d"
        store.getLong(KEY, 1L).shouldBeInstanceOf<AndroidPreference.LongPrimitive>().get() shouldBe 1L
        store.getInt(KEY, 2).shouldBeInstanceOf<AndroidPreference.IntPrimitive>().get() shouldBe 2
        store.getFloat(KEY, 3f).shouldBeInstanceOf<AndroidPreference.FloatPrimitive>().get() shouldBe 3f
        store.getBoolean(KEY, true).shouldBeInstanceOf<AndroidPreference.BooleanPrimitive>().get() shouldBe true
        val set = store.getStringSet(KEY, setOf("s")).shouldBeInstanceOf<AndroidPreference.StringSetPrimitive>()
        set.get() shouldBe setOf("s")
    }

    @Test
    fun objectFactories() {
        val asString = store.getObjectFromString(
            key = KEY,
            defaultValue = 1,
            serializer = { it.toString() },
            deserializer = { it.toInt() },
        )
        asString.shouldBeInstanceOf<AndroidPreference.ObjectAsString<*>>()
        asString.set(4)
        asString.get() shouldBe 4

        val asInt = store.getObjectFromInt(key = KEY, defaultValue = 1, serializer = { it }, deserializer = { it })
        asInt.shouldBeInstanceOf<AndroidPreference.ObjectAsInt<*>>()
        asInt.set(5)
        asInt.get() shouldBe 5

        val asSet = store.getObjectSetFromStringSet(
            key = KEY,
            defaultValue = setOf(1),
            serializer = { it.toString() },
            deserializer = { it.toIntOrNull() },
        )
        asSet.shouldBeInstanceOf<AndroidPreference.ObjectSetAsStringSet<*>>()
        asSet.set(setOf(6))
        asSet.get() shouldBe setOf(6)
    }

    @Test
    fun getAllMirrorsSharedPreferences() {
        prefs.edit { putString(KEY, "v") }
        store.getAll() shouldBe mapOf(KEY to "v")
    }

    @Test
    fun keyFlowDeliversListenerEvents() {
        val pref = store.getString(KEY, "d")
        runBlocking {
            val collected = async { pref.changes().take(2).toList() }
            repeat(SETTLE_YIELDS) { yield() }
            pref.set("x")
            withTimeout(TIMEOUT_MS) { collected.await() } shouldBe listOf("d", "x")
        }
    }
}
