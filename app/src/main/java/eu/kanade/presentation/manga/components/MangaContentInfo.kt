package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.api.get

@Composable
internal fun ColumnScope.MangaContentInfo(
    title: String,
    author: String?,
    artist: String?,
    status: Long,
    sourceName: String,
    isStubSource: Boolean,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign? = LocalTextStyle.current.textAlign,
) {
    val context = LocalContext.current
    Text(
        text = title.ifBlank { stringResource(MR.strings.unknown_title) },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.clickableNoIndication(
            onLongClick = { if (title.isNotBlank()) context.copyToClipboard(title, title) },
            onClick = { if (title.isNotBlank()) doSearch(title, true) },
        ),
        textAlign = textAlign,
    )
    Spacer(modifier = Modifier.height(2.dp))
    PersonRow(
        icon = Icons.Filled.PersonOutline,
        name = author?.takeIf { it.isNotBlank() } ?: stringResource(MR.strings.unknown_author),
        searchable = author.takeUnless { it.isNullOrBlank() },
        doSearch = doSearch,
        textAlign = textAlign,
    )
    if (!artist.isNullOrBlank() && author != artist) {
        PersonRow(
            icon =
            Icons.Filled.Brush,
            name = artist, searchable = artist, doSearch = doSearch, textAlign = textAlign,
        )
    }
    Spacer(modifier = Modifier.height(2.dp))
    StatusRow(status, sourceName, isStubSource, doSearch)
}

// Author or artist: tap searches globally, long-press copies; [searchable] is null when the name is a placeholder.
@Composable
internal fun PersonRow(
    icon: ImageVector,
    name: String,
    searchable: String?,
    doSearch: (query: String, global: Boolean) -> Unit,
    textAlign: TextAlign?,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.secondaryItemAlpha(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .clickableNoIndication(
                    onLongClick = { searchable?.let { context.copyToClipboard(it, it) } },
                    onClick = { searchable?.let { doSearch(it, true) } },
                ),
            textAlign = textAlign,
        )
    }
}

@Composable
internal fun StatusRow(status: Long, sourceName: String, isStubSource: Boolean, doSearch: (String, Boolean) -> Unit) {
    Row(
        modifier = Modifier.secondaryItemAlpha(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val presentation = mangaStatusPresentation(status)
        Icon(
            imageVector = presentation.icon,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(16.dp),
        )
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Text(
                text = stringResource(presentation.label),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            DotSeparatorText()
            if (isStubSource) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = sourceName,
                modifier = Modifier.clickableNoIndication { doSearch(sourceName, false) },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}
