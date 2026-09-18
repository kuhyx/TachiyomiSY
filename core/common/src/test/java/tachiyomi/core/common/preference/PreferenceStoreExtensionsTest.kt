package tachiyomi.core.common.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.lang.reflect.InvocationTargetException

private const val FACADE = "tachiyomi.core.common.preference.PreferenceStoreKt"
private const val KEY = "k"
private const val SECURE_SCREEN_KEY = "secure_screen_v2"

/**
 * The [PreferenceStore] helpers over a real store, so the serializer and deserializer lambdas run.
 *
 * [getEnum] and [getEnumSet] are reified inline functions: their compiled copies throw
 * [UnsupportedOperationException] on entry, so only copies inlined into main code
 * (`SecurityPreferences` for [getEnum]) can be executed; [getEnumSet] keeps its logic in a
 * non-reified overload that runs here.
 */
@RunWith(RobolectricTestRunner::class)
internal class PreferenceStoreExtensionsTest {
    private lateinit var prefs: SharedPreferences
    private lateinit var store: AndroidPreferenceStore

    @Before
    fun setUp() {
        prefs = robolectricPrefs()
        store = AndroidPreferenceStore(RuntimeEnvironment.getApplication(), prefs)
    }

    @Test
    fun longArrayRoundTripsAsCsv() {
        val pref = store.getLongArray(KEY, listOf(1L))
        pref.get() shouldBe listOf(1L)
        pref.set(listOf(2L, 3L))
        prefs.getString(KEY, null) shouldBe "2,3"
        pref.get() shouldBe listOf(2L, 3L)
    }

    @Test
    fun longArraySkipsUnparseable() {
        prefs.edit { putString(KEY, "4,x,5") }
        store.getLongArray(KEY, emptyList()).get() shouldBe listOf(4L, 5L)
    }

    @Test
    fun enumInlinedRoundTrip() {
        val pref = store.getEnum(KEY, TriState.DISABLED)
        pref.get() shouldBe TriState.DISABLED
        pref.set(TriState.ENABLED_NOT)
        prefs.getString(KEY, null) shouldBe "ENABLED_NOT"
        pref.get() shouldBe TriState.ENABLED_NOT
        prefs.edit { putString(KEY, "bogus") }
        pref.get() shouldBe TriState.DISABLED
    }

    @Test
    fun enumInlinedIntoMainCode() {
        val security = SecurityPreferences(store)
        security.secureScreen.get() shouldBe SecurityPreferences.SecureScreenMode.INCOGNITO
        security.secureScreen.set(SecurityPreferences.SecureScreenMode.ALWAYS)
        prefs.getString(SECURE_SCREEN_KEY, null) shouldBe "ALWAYS"
        security.secureScreen.get() shouldBe SecurityPreferences.SecureScreenMode.ALWAYS
        prefs.edit { putString(SECURE_SCREEN_KEY, "bogus") }
        security.secureScreen.get() shouldBe SecurityPreferences.SecureScreenMode.INCOGNITO
    }

    @Test
    fun enumSetInlinedRoundTrip() {
        val pref = store.getEnumSet(KEY, setOf(TriState.DISABLED))
        pref.get() shouldBe setOf(TriState.DISABLED)
        pref.set(setOf(TriState.ENABLED_IS, TriState.ENABLED_NOT))
        prefs.getStringSet(KEY, null) shouldBe setOf("ENABLED_IS", "ENABLED_NOT")
        pref.get() shouldBe setOf(TriState.ENABLED_IS, TriState.ENABLED_NOT)
        prefs.edit { putStringSet(KEY, setOf("ENABLED_IS", "bogus")) }
        pref.get() shouldBe setOf(TriState.ENABLED_IS)
    }

    @Test
    fun enumSetOverValuesDropsUnknown() {
        val pref = store.getEnumSet(KEY, setOf(TriState.DISABLED), TriState.entries.toTypedArray())
        prefs.edit { putStringSet(KEY, setOf("ENABLED_NOT", "bogus")) }
        pref.get() shouldBe setOf(TriState.ENABLED_NOT)
    }

    @Test
    fun enumSetHasNoCallableCopy() {
        val types = listOf(PreferenceStore::class.java, String::class.java, Set::class.java)
        val method = staticMethod(FACADE, "getEnumSetReified", types)
        val failure = shouldThrow<InvocationTargetException> {
            method.callStatic(listOf(store, KEY, setOf(TriState.DISABLED)))
        }
        failure.targetException.shouldBeInstanceOf<UnsupportedOperationException>()
    }
}
