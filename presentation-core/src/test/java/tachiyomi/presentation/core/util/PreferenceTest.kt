package tachiyomi.presentation.core.util

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

/** A [Preference] whose [changes] really emit, unlike the in-memory store's. */
private class FlowPreference(private val initial: Int) : Preference<Int> {
    private val flow = MutableStateFlow(initial)

    override fun key(): String = "flow"

    override fun get(): Int = flow.value

    override fun set(value: Int) {
        flow.value = value
    }

    override fun isSet(): Boolean = flow.value != initial

    override fun delete() {
        flow.value = initial
    }

    override fun defaultValue(): Int = initial

    override fun changes(): Flow<Int> = flow

    override fun stateIn(scope: CoroutineScope): StateFlow<Int> =
        flow.stateIn(scope, SharingStarted.Eagerly, flow.value)
}

@RunWith(RobolectricTestRunner::class)
internal class PreferenceTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: State<Int>
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun Host(pref: Preference<Int>, tick: Int) {
        Text(text = "tick $tick")
        state = pref.collectAsState()
    }

    @Test
    fun startsFromTheStoredValue() {
        val pref = InMemoryPreferenceStore().getBoolean("k", false)
        pref.set(true)
        var seen = false
        compose.setContent { seen = pref.collectAsState().value }
        seen shouldBe true
    }

    @Test
    fun followsLaterChanges() {
        val pref = FlowPreference(1)
        compose.setContent { state = pref.collectAsState() }
        state.value shouldBe 1
        pref.set(2)
        compose.waitForIdle()
        state.value shouldBe 2
    }

    @Test
    fun switchesFlowWithPreference() {
        val first = FlowPreference(1)
        val second = FlowPreference(5)
        var pref by mutableStateOf<Preference<Int>>(first)
        compose.setContent { state = pref.collectAsState() }
        state.value shouldBe 1
        compose.runOnIdle { pref = second }
        compose.waitForIdle()
        state.value shouldBe 5
        second.set(6)
        compose.waitForIdle()
        state.value shouldBe 6
    }

    @Test
    fun hostParametersRecompose() {
        val first = FlowPreference(1)
        val second = FlowPreference(5)
        var pref by mutableStateOf<Preference<Int>>(first)
        compose.setContent { Host(pref = pref, tick = tick) }
        state.value shouldBe 1
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        state.value shouldBe 1
        compose.runOnIdle { pref = second }
        compose.waitForIdle()
        state.value shouldBe 5
    }

    @Test
    fun keepsFlowAcrossRecompose() {
        val pref = FlowPreference(3)
        compose.setContent {
            Column {
                Text(text = "tick $tick")
                state = pref.collectAsState()
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        state.value shouldBe 3
        pref.set(4)
        compose.waitForIdle()
        state.value shouldBe 4
    }
}
