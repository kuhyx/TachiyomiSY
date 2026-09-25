package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState

@RunWith(RobolectricTestRunner::class)
internal class SourceFilterItemsTest {
    @get:Rule
    val compose = createComposeRule()

    private var updates = 0
    private val onUpdate: () -> Unit = { updates++ }

    private class Check : Filter.CheckBox("Check")
    private class Tri : Filter.TriState("Tri")
    private class Words : Filter.Text("Words")
    private class Pick : Filter.Select<String>("Pick", arrayOf("One", "Two"))
    private class Order(state: Selection?) : Filter.Sort("Order", arrayOf("Alpha", "Beta"), state)

    @Test
    fun checkboxToggles() {
        val filter = Check()
        compose.setContent { MaterialTheme { CheckboxFilterItem(filter, onUpdate) } }
        compose.onNodeWithText("Check").performClick()
        filter.state shouldBe true
        updates shouldBe 1
    }

    @Test
    fun triStateCycles() {
        val filter = Tri()
        compose.setContent { MaterialTheme { TriStateFilterItem(filter, onUpdate) } }
        compose.onNodeWithText("Tri").performClick()
        filter.state shouldBe Filter.TriState.STATE_INCLUDE
        updates shouldBe 1
    }

    @Test
    fun textIsWritten() {
        val filter = Words()
        compose.setContent { MaterialTheme { TextFilterItem(filter, onUpdate) } }
        compose.onNodeWithText("Words").performTextReplacement("abc")
        filter.state shouldBe "abc"
        updates shouldBe 1
    }

    @Test
    fun selectPicksAnOption() {
        val filter = Pick()
        compose.setContent { MaterialTheme { SelectFilterItem(filter, onUpdate) } }
        compose.onNodeWithText("One").performClick()
        compose.onNodeWithText("Two").performClick()
        filter.state shouldBe 1
        updates shouldBe 1
    }

    @Test
    fun sortTogglesTheSameColumn() {
        val filter = Order(Filter.Sort.Selection(index = 0, ascending = true))
        compose.setContent { MaterialTheme { SortFilterItem(filter, onUpdate, startExpanded = true) } }
        compose.onNodeWithText("Alpha").performClick()
        filter.state shouldBe Filter.Sort.Selection(index = 0, ascending = false)
        compose.onNodeWithText("Beta").performClick()
        filter.state shouldBe Filter.Sort.Selection(index = 1, ascending = false)
        updates shouldBe 2
    }

    @Test
    fun sortWithoutStateAscends() {
        val filter = Order(null)
        compose.setContent { MaterialTheme { SortFilterItem(filter, onUpdate, startExpanded = true) } }
        compose.onNodeWithText("Beta").performClick()
        filter.state shouldBe Filter.Sort.Selection(index = 1, ascending = true)
    }

    @Test
    fun triStateMapsBothWays() {
        Filter.TriState.STATE_IGNORE.toTriStateFilter() shouldBe TriState.DISABLED
        Filter.TriState.STATE_INCLUDE.toTriStateFilter() shouldBe TriState.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE.toTriStateFilter() shouldBe TriState.ENABLED_NOT
        shouldThrow<IllegalStateException> { 7.toTriStateFilter() }
        TriState.entries.map { it.toTriStateInt() } shouldBe listOf(0, 1, 2)
    }
}
