package eu.kanade.tachiyomi.data.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SharedPreferencesDataStoreTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var store: SharedPreferencesDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("datastore-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = SharedPreferencesDataStore(prefs)
    }

    @Test
    fun booleanRoundTrips() {
        store.getBoolean("b", true) shouldBe true
        store.putBoolean("b", false)
        store.getBoolean("b", true) shouldBe false
        prefs.getBoolean("b", true) shouldBe false
    }

    @Test
    fun intRoundTrips() {
        store.getInt("i", 7) shouldBe 7
        store.putInt("i", 42)
        store.getInt("i", 7) shouldBe 42
    }

    @Test
    fun longRoundTrips() {
        store.getLong("l", 9L) shouldBe 9L
        store.putLong("l", 11L)
        store.getLong("l", 9L) shouldBe 11L
    }

    @Test
    fun floatRoundTrips() {
        store.getFloat("f", 1.5f) shouldBe 1.5f
        store.putFloat("f", 2.5f)
        store.getFloat("f", 1.5f) shouldBe 2.5f
    }

    @Test
    fun stringRoundTrips() {
        store.getString("s", "default") shouldBe "default"
        store.putString("s", "value")
        store.getString("s", "default") shouldBe "value"
    }

    @Test
    fun stringAcceptsNulls() {
        store.getString(null, null).shouldBeNull()
        store.putString("s", null)
        // A null value removes the key, so the default comes back.
        store.getString("s", "default") shouldBe "default"
    }

    @Test
    fun stringSetRoundTrips() {
        store.getStringSet("set", mutableSetOf("a"))!! shouldContainExactly setOf("a")
        store.putStringSet("set", mutableSetOf("b", "c"))
        store.getStringSet("set", mutableSetOf())!! shouldContainExactly setOf("b", "c")
    }

    @Test
    fun stringSetAcceptsNulls() {
        store.getStringSet("absent", null).shouldBeNull()
        store.putStringSet("set", null)
        store.getStringSet("set", mutableSetOf("fallback"))!! shouldContainExactly setOf("fallback")
    }

    @Test
    fun nullKeysAreAccepted() {
        store.putBoolean(null, true)
        store.getBoolean(null, false) shouldBe true
        store.putInt(null, 1)
        store.getInt(null, 0) shouldBe 1
        store.putLong(null, 1L)
        store.getLong(null, 0L) shouldBe 1L
        store.putFloat(null, 1f)
        store.getFloat(null, 0f) shouldBe 1f
        store.putStringSet(null, mutableSetOf("x"))
        store.getStringSet(null, mutableSetOf())!! shouldContainExactly setOf("x")
    }
}
