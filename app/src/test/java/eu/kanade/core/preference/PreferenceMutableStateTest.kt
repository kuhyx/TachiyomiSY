package eu.kanade.core.preference

import eu.kanade.domain.FlowPreferenceStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class PreferenceMutableStateTest {

    @Test
    fun mirrorsThePreferenceBothWays() = runTest(UnconfinedTestDispatcher()) {
        val preference = FlowPreferenceStore().getInt("n", 1)
        val state = preference.asState(backgroundScope)
        state.value shouldBe 1
        state.value = 5
        preference.get() shouldBe 5
        state.value shouldBe 5
        val (value, setter) = state
        value shouldBe 5
        setter(7)
        preference.get() shouldBe 7
        preference.set(9)
        state.component1() shouldBe 9
    }
}
