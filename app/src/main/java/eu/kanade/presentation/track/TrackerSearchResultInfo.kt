package eu.kanade.presentation.track

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

// Title, long-press menu, creators and the detail rows that have a value.
@Composable
internal fun SearchResultInfo(
    trackSearch: TrackSearch,
    menuExpanded: Boolean,
    onCollapseMenu: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard: Clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val type = trackSearch.publishingType.toLowerCase(Locale.current).capitalize(Locale.current)
    val status = trackSearch.publishingStatus.toLowerCase(Locale.current).capitalize(Locale.current)
    Column {
        Text(
            text = trackSearch.title,
            modifier = Modifier.padding(end = 28.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
        SearchResultItemDropDownMenu(
            expanded = menuExpanded,
            onCollapseMenu = onCollapseMenu,
            onCopyName = {
                scope.launch {
                    val clipEntry = ClipData.newPlainText(
                        trackSearch.title,
                        trackSearch.title,
                    ).toClipEntry()
                    clipboard.setClipEntry(clipEntry)
                }
            },
            onOpenInBrowser = {
                val url = trackSearch.trackingUrl
                if (url.isNotBlank()) {
                    context.openInBrowser(url)
                }
            },
        )
        if (trackSearch.authors.isNotEmpty() || trackSearch.artists.isNotEmpty()) {
            Text(
                text = (trackSearch.authors + trackSearch.artists).distinct().joinToString(),
                modifier = Modifier.secondaryItemAlpha(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val details = listOf(
            MR.strings.track_type to type,
            MR.strings.label_started to trackSearch.startDate,
            MR.strings.track_status to status,
            MR.strings.score to trackSearch.score.takeIf { it != -1.0 }?.toString().orEmpty(),
        )
        details.filter { (_, text) -> text.isNotBlank() }.forEach { (title, text) ->
            SearchResultItemDetails(
                title = stringResource(title),
                text = text,
            )
        }
    }
}

@Composable
internal fun SearchResultItemDropDownMenu(
    expanded: Boolean,
    onCollapseMenu: () -> Unit,
    onCopyName: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onCollapseMenu,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(MR.strings.action_copy_to_clipboard)) },
            onClick = {
                onCopyName()
                onCollapseMenu()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(MR.strings.action_open_in_browser)) },
            onClick = {
                onOpenInBrowser()
            },
        )
    }
}

@Composable
internal fun SearchResultItemDetails(
    title: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
        Text(
            text = title,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .secondaryItemAlpha(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
