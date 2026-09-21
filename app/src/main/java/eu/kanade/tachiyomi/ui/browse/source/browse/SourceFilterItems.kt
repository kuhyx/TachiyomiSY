package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import eu.kanade.tachiyomi.source.model.Filter
import tachiyomi.core.common.preference.TriState
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem

@Composable
internal fun CheckboxFilterItem(filter: Filter.CheckBox, onUpdate: () -> Unit) {
    CheckboxItem(
        label = filter.name,
        checked = filter.state,
    ) {
        filter.state = !filter.state
        onUpdate()
    }
}

@Composable
internal fun TriStateFilterItem(filter: Filter.TriState, onUpdate: () -> Unit) {
    TriStateItem(
        label = filter.name,
        state = filter.state.toTriStateFilter(),
    ) {
        filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
        onUpdate()
    }
}

@Composable
internal fun TextFilterItem(filter: Filter.Text, onUpdate: () -> Unit) {
    TextItem(
        label = filter.name,
        value = filter.state,
    ) {
        filter.state = it
        onUpdate()
    }
}

@Composable
internal fun SelectFilterItem(filter: Filter.Select<*>, onUpdate: () -> Unit) {
    SelectItem(
        label = filter.name,
        options = filter.values,
        selectedIndex = filter.state,
    ) {
        filter.state = it
        onUpdate()
    }
}

internal fun Int.toTriStateFilter(): TriState {
    return when (this) {
        Filter.TriState.STATE_IGNORE -> TriState.DISABLED
        Filter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> error("Unknown TriState state: $this")
    }
}

internal fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> Filter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> Filter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> Filter.TriState.STATE_EXCLUDE
    }
}

@Composable
internal fun SortFilterItem(filter: Filter.Sort, onUpdate: () -> Unit, startExpanded: Boolean) {
    CollapsibleBox(
        heading = filter.name,
        // SY -->
        startExpanded = startExpanded,
        // SY <--
    ) {
        Column {
            filter.values.mapIndexed { index, item ->
                val sortAscending = filter.state?.ascending
                    ?.takeIf { index == filter.state?.index }
                SortItem(
                    label = item,
                    sortDescending = sortAscending?.let { !it },
                    onClick = {
                        val ascending = if (index == filter.state?.index) {
                            !filter.state!!.ascending
                        } else {
                            filter.state?.ascending ?: true
                        }
                        filter.state = Filter.Sort.Selection(index = index, ascending = ascending)
                        onUpdate()
                    },
                )
            }
        }
    }
}
