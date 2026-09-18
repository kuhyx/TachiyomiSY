package tachiyomi.core.common.preference

import android.content.Context
import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

private const val KEY = "k"

/** The `?:` fallbacks that a real [SharedPreferences] never exercises: a null read of a non-null default. */
internal class AndroidPreferenceMockTest {
    private val prefs = mockk<SharedPreferences>()

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun nullStringUsesDefault() {
        every { prefs.getString(KEY, "d") } returns null
        AndroidPreference.StringPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = "d",
        ).get() shouldBe "d"
    }

    @Test
    fun nullStringSetUsesDefault() {
        every { prefs.getStringSet(KEY, setOf("d")) } returns null
        AndroidPreference.StringSetPrimitive(
            preferences = prefs,
            keyFlow = emptyFlow(),
            key = KEY,
            defaultValue = setOf("d"),
        ).get() shouldBe setOf("d")
    }

    @Test
    fun nullAllBecomesEmptyMap() {
        every { prefs.all } returns null
        AndroidPreferenceStore(mockk<Context>(), prefs).getAll() shouldBe emptyMap<String, Any>()
    }
}
