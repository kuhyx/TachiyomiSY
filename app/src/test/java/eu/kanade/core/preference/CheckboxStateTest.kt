package eu.kanade.core.preference

import androidx.compose.ui.state.ToggleableState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.CheckboxState

internal class CheckboxStateTest {

    @Test
    fun mapsEachStateOntoTheToggle() {
        CheckboxState.TriState.Exclude("x").asToggleableState() shouldBe ToggleableState.Indeterminate
        CheckboxState.TriState.Include("x").asToggleableState() shouldBe ToggleableState.On
        CheckboxState.TriState.None("x").asToggleableState() shouldBe ToggleableState.Off
    }
}
