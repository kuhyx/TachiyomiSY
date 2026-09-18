package tachiyomi.core.common.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val KEY = "k"

@RunWith(RobolectricTestRunner::class)
internal class AndroidPreferenceFlowTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = robolectricPrefs()
        prefs.edit { putString(KEY, "first") }
    }

    private fun pref(keyFlow: Flow<String?>) = AndroidPreference.StringPrimitive(
        preferences = prefs,
        keyFlow = keyFlow,
        key = KEY,
        defaultValue = "d",
    )

    // Emits key after storing value under KEY, so a re-read is observable.
    private fun keyAfterWrite(key: String?, value: String): Flow<String?> = flow {
        prefs.edit { putString(KEY, value) }
        emit(key)
    }

    @Test
    fun changesStartWithCurrentValue() {
        runBlocking { pref(flowOf()).changes().toList() } shouldBe listOf("first")
    }

    @Test
    fun changesReReadOnOwnKey() {
        runBlocking { pref(keyAfterWrite(KEY, "second")).changes().toList() } shouldBe listOf("first", "second")
    }

    @Test
    fun changesReReadOnNullKey() {
        runBlocking { pref(keyAfterWrite(null, "third")).changes().toList() } shouldBe listOf("first", "third")
    }

    @Test
    fun changesIgnoreOtherKeys() {
        runBlocking { pref(keyAfterWrite("other", "fourth")).changes().toList() } shouldBe listOf("first")
    }

    @Test
    fun stateInStartsAtCurrentValue() {
        runBlocking {
            val scope = CoroutineScope(coroutineContext + Job())
            val state = pref(flowOf()).stateIn(scope)
            repeat(3) { yield() }
            state.value shouldBe "first"
            scope.cancel()
        }
    }
}
