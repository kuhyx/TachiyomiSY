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
internal class AndroidPreferencePrimitivesTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = robolectricPrefs()
    }

    private fun <T> Preference<T>.roundTrip(value: T, default: T) {
        key() shouldBe KEY
        defaultValue() shouldBe default
        isSet() shouldBe false
        get() shouldBe default
        set(value)
        isSet() shouldBe true
        get() shouldBe value
        delete()
        isSet() shouldBe false
        get() shouldBe default
    }

    @Test
    fun stringPrimitive() {
        AndroidPreference.StringPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = "d",
        ).roundTrip("v", "d")
    }

    @Test
    fun longPrimitive() {
        AndroidPreference.LongPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = 1L,
        ).roundTrip(2L, 1L)
    }

    @Test
    fun intPrimitive() {
        AndroidPreference.IntPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = 1,
        ).roundTrip(2, 1)
    }

    @Test
    fun floatPrimitive() {
        AndroidPreference.FloatPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = 1f,
        ).roundTrip(2.5f, 1f)
    }

    @Test
    fun booleanPrimitive() {
        AndroidPreference.BooleanPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = false,
        ).roundTrip(true, false)
    }

    @Test
    fun stringSetPrimitive() {
        AndroidPreference.StringSetPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = setOf("d"),
        ).roundTrip(setOf("a", "b"), setOf("d"))
    }

    @Test
    fun wrongTypeIsDeletedAndDefaulted() {
        prefs.edit { putString(KEY, "not an int") }
        val pref = AndroidPreference.IntPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = 7,
        )
        pref.get() shouldBe 7
        prefs.contains(KEY) shouldBe false
        pref.isSet() shouldBe false
    }
}
