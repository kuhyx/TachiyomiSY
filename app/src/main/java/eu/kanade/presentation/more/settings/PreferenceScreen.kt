package eu.kanade.presentation.more.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items.
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single
 * [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 * @param contentPadding padding applied inside the scrolling list
 */
@Composable
internal fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            state.scrollToHighlighted(items, highlightKey)
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                is Preference.PreferenceGroup -> preferenceGroup(
                    preference,
                    highlightKey = highlightKey,
                    spacerAfter = i < items.lastIndex,
                )
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

// Scrolls to the setting the search result pointed at, then forgets it so the next visit starts at the top.
private suspend fun LazyListState.scrollToHighlighted(items: List<Preference>, highlightKey: String) {
    val i = items.findHighlightedIndex(highlightKey)
    if (i >= 0) {
        delay(0.5.seconds)
        animateScrollToItem(i)
    }
    SearchableSettings.highlightKey = null
}

// Header, the group's items, then a spacer slot; a disabled group adds nothing.
private fun LazyListScope.preferenceGroup(
    preference: Preference.PreferenceGroup,
    highlightKey: String?,
    spacerAfter: Boolean,
) {
    if (!preference.enabled) return
    item {
        Column {
            PreferenceGroupHeader(title = preference.title)
        }
    }
    items(preference.preferenceItems) { item ->
        PreferenceItem(
            item = item,
            highlightKey = highlightKey,
        )
    }
    item {
        if (spacerAfter) {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(null) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}
