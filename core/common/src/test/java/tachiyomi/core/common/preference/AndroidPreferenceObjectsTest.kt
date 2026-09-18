package tachiyomi.core.common.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val KEY = "k"

@RunWith(RobolectricTestRunner::class)
internal class AndroidPreferenceObjectsTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = robolectricPrefs()
    }

    private fun intAsString(deserializer: (String) -> Int = { it.toInt() }) = AndroidPreference.ObjectAsString(
        preferences = prefs,
        keyFlow = emptyFlow(),
        key = KEY,
        defaultValue = 1,
        serializer = { it.toString() },
        deserializer = deserializer,
    )

    private fun intAsInt(deserializer: (Int) -> Int = { it * 2 }) = AndroidPreference.ObjectAsInt(
        preferences = prefs,
        keyFlow = emptyFlow(),
        key = KEY,
        defaultValue = 1,
        serializer = { it / 2 },
        deserializer = deserializer,
    )

    private fun intSet(deserializer: (String) -> Int? = { it.toIntOrNull() }) = AndroidPreference.ObjectSetAsStringSet(
        preferences = prefs,
        keyFlow = emptyFlow(),
        key = KEY,
        defaultValue = setOf(1),
        serializer = { it.toString() },
        deserializer = deserializer,
    )

    @Test
    fun objectAsStringRoundTrip() {
        val pref = intAsString()
        pref.get() shouldBe 1
        pref.set(5)
        prefs.getString(KEY, null) shouldBe "5"
        pref.get() shouldBe 5
    }

    @Test
    fun objectAsStringNullDeserialize() {
        val pref = AndroidPreference.ObjectAsString<String?>(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = "d",
            serializer = { it.orEmpty() },
            deserializer = { null },
        )
        pref.set("v")
        pref.get() shouldBe "d"
    }

    @Test
    fun objectAsStringBadValue() {
        prefs.edit { putString(KEY, "x") }
        intAsString().get() shouldBe 1
    }

    @Test
    fun objectAsIntRoundTrip() {
        val pref = intAsInt()
        pref.get() shouldBe 1
        pref.set(6)
        prefs.getInt(KEY, 0) shouldBe 3
        pref.get() shouldBe 6
    }

    @Test
    fun objectAsIntBadValue() {
        prefs.edit { putInt(KEY, 4) }
        intAsInt { error("boom") }.get() shouldBe 1
    }

    @Test
    fun objectSetRoundTrip() {
        val pref = intSet()
        pref.get() shouldBe setOf(1)
        pref.set(setOf(2, 3))
        prefs.getStringSet(KEY, null) shouldBe setOf("2", "3")
        pref.get() shouldBe setOf(2, 3)
    }

    @Test
    fun objectSetDropsUnreadable() {
        prefs.edit { putStringSet(KEY, setOf("2", "x")) }
        intSet().get() shouldBe setOf(2)
    }

    @Test
    fun objectSetBadValue() {
        prefs.edit { putStringSet(KEY, setOf("2")) }
        intSet { error("boom") }.get() shouldBe setOf(1)
    }
}
