package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.source.model.Filter
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unchecking a checked box, and a descending sort column flipping back to ascending. */
@RunWith(RobolectricTestRunner::class)
internal class SourceFilterItemsMoreTest {
    @get:Rule
    val compose = createComposeRule()

    private var updates = 0
    private val onUpdate: () -> Unit = { updates++ }

    private class Check : Filter.CheckBox("Check", true)
    private class Order(state: Selection?) : Filter.Sort("Order", arrayOf("Alpha", "Beta"), state)

    @Test
    fun checkedBoxUnchecks() {
        val filter = Check()
        compose.setContent { MaterialTheme { CheckboxFilterItem(filter, onUpdate) } }
        compose.onNodeWithText("Check").performClick()
        filter.state shouldBe false
        updates shouldBe 1
    }

    @Test
    fun descendingFlipsToAscending() {
        val filter = Order(Filter.Sort.Selection(index = 0, ascending = false))
        compose.setContent { MaterialTheme { SortFilterItem(filter, onUpdate, startExpanded = true) } }
        compose.onNodeWithText("Alpha").performClick()
        filter.state shouldBe Filter.Sort.Selection(index = 0, ascending = true)
        updates shouldBe 1
    }
}
