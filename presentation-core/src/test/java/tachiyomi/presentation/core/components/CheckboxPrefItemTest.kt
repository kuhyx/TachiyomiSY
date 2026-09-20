package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

@Composable
private fun CheckboxPrefHost(pref: Preference<Boolean>, tick: Int) {
    Column {
        Text("tick $tick")
        CheckboxItem(label = "Pref", pref = pref)
    }
}

/** A stable [Preference] that is equal to any other with the same key, whatever the instance. */
@Stable
private class StatePref(private val key: String, private val state: MutableState<Boolean>) : Preference<Boolean> {
    override fun equals(other: Any?): Boolean = other is StatePref && other.key == key

    override fun hashCode(): Int = key.hashCode()

    override fun key(): String = key
    override fun get(): Boolean = state.value
    override fun set(value: Boolean) {
        state.value = value
    }
    override fun isSet(): Boolean = true
    override fun delete() {
        state.value = false
    }
    override fun defaultValue(): Boolean = false
    override fun changes(): Flow<Boolean> = flowOf(state.value)
    override fun stateIn(scope: CoroutineScope): StateFlow<Boolean> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())
}

/**
 * Recomposition shapes of the pref-backed [CheckboxItem]: the same pref instance again, then a new
 * one, straight from state and through a host composable; and a stable, equal replacement, which
 * Compose skips, so the row keeps toggling the pref it was built with.
 */
@RunWith(RobolectricTestRunner::class)
internal class CheckboxPrefItemTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = InMemoryPreferenceStore()

    @Test
    fun samePrefThenNewPrefFromState() {
        val first = store.getBoolean("first", false)
        val second = store.getBoolean("second", false)
        var pref by mutableStateOf(first)
        var tick by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    CheckboxItem(label = "Pref", pref = pref)
                }
            }
        }
        compose.waitForIdle()
        tick++
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle { first.get() shouldBe true }
        pref = second
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle {
            second.get() shouldBe true
            first.get() shouldBe true
        }
    }

    @Test
    fun equalStablePrefIsSkipped() {
        val first = StatePref("shared", mutableStateOf(false))
        val second = StatePref("shared", mutableStateOf(false))
        // Structural equality would swallow the swap to an equal instance; compare by reference.
        var pref by mutableStateOf(first, referentialEqualityPolicy())
        var tick by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    CheckboxItem(label = "Pref", pref = pref)
                }
            }
        }
        compose.waitForIdle()
        tick++
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle { first.get() shouldBe true }
        pref = second
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle {
            first.get() shouldBe false
            second.get() shouldBe false
        }
    }

    @Test
    fun samePrefThenNewPrefThroughHost() {
        val first = store.getBoolean("first", false)
        val second = store.getBoolean("second", false)
        var pref by mutableStateOf(first)
        var tick by mutableIntStateOf(0)
        compose.setContent { MaterialTheme { CheckboxPrefHost(pref = pref, tick = tick) } }
        compose.waitForIdle()
        tick++
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle { first.get() shouldBe true }
        pref = second
        compose.waitForIdle()
        compose.onNodeWithText("Pref").performClick()
        compose.runOnIdle {
            second.get() shouldBe true
            first.get() shouldBe true
        }
    }
}
