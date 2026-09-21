package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DropdownMenu
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

// The tag chips: a scrolling row when collapsed, a wrapping grid (or SY namespace groups) when expanded. Any tap
// opens the search/copy menu for that tag.
@Composable
internal fun TagsSection(
    tags: List<String>,
    expanded: Boolean,
    searchMetadataChips: SearchMetadataChips?,
    onTagSearch: (String) -> Unit,
    onGlobalSearch: (String) -> Unit,
    onCopyTagToClipboard: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .padding(vertical = 12.dp)
            .animateContentSize(animationSpec = spring())
            .fillMaxWidth(),
    ) {
        var showMenu by remember { mutableStateOf(false) }
        var tagSelected by remember { mutableStateOf("") }
        val onTagClick: (String) -> Unit = {
            tagSelected = it
            showMenu = true
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            TagMenuItem(MR.strings.action_search) {
                onTagSearch(tagSelected)
                showMenu = false
            }
            // SY -->
            TagMenuItem(MR.strings.action_global_search) {
                onGlobalSearch(tagSelected)
                showMenu = false
            }
            // SY <--
            TagMenuItem(MR.strings.action_copy_to_clipboard) {
                onCopyTagToClipboard(tagSelected)
                showMenu = false
            }
        }
        when {
            // SY -->
            expanded && searchMetadataChips != null -> NamespaceTags(tags = searchMetadataChips, onClick = onTagClick)
            // SY <--
            expanded -> FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                tags.forEach {
                    TagsChip(modifier = DefaultTagChipModifier, text = it, onClick = { onTagClick(it) })
                }
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                items(items = tags) {
                    TagsChip(modifier = DefaultTagChipModifier, text = it, onClick = { onTagClick(it) })
                }
            }
        }
    }
}

@Composable
internal fun TagMenuItem(label: StringResource, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(label)) },
        onClick = onClick,
    )
}
