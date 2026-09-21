package eu.kanade.presentation.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import coil3.request.crossfade
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.manga.components.mangaStatusPresentation
import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import uy.kohesive.injekt.api.get

@Composable
internal fun DuplicateMangaListItem(
    duplicate: MangaWithChapterCount,
    getSource: () -> Source,
    onDismissRequest: () -> Unit,
    onOpenManga: () -> Unit,
    onMigrate: () -> Unit,
) {
    val source = getSource()
    val manga = duplicate.manga
    Column(
        modifier = Modifier
            .width(MangaCardWidth)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onLongClick = { onOpenManga() },
                onClick = {
                    onDismissRequest()
                    onMigrate()
                },
            )
            .padding(MaterialTheme.padding.small),
    ) {
        CoverWithChapterCount(duplicate)

        Spacer(modifier = Modifier.height(MaterialTheme.padding.extraSmall))

        Text(
            text = manga.title,
            style = MaterialTheme.typography.titleSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )

        if (!manga.author.isNullOrBlank()) {
            MangaDetailRow(
                text = manga.author!!,
                iconImageVector = Icons.Filled.PersonOutline,
                maxLines = 2,
            )
        }

        if (!manga.artist.isNullOrBlank() && manga.author != manga.artist) {
            MangaDetailRow(
                text = manga.artist!!,
                iconImageVector = Icons.Filled.Brush,
                maxLines = 2,
            )
        }

        StatusRow(manga.status)

        Spacer(modifier = Modifier.weight(1f))

        SourceNameRow(source)
    }
}

// Source name, with a warning icon when the source is no longer installed.
@Composable
internal fun SourceNameRow(source: Source) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (source is StubSource) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = source.name,
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CoverWithChapterCount(duplicate: MangaWithChapterCount) {
    Box {
        MangaCover.Book(
            data = ImageRequest.Builder(LocalContext.current)
                .data(duplicate.manga)
                .crossfade(true)
                .build(),
            modifier = Modifier.fillMaxWidth(),
        )
        BadgeGroup(
            modifier = Modifier
                .padding(4.dp)
                .align(Alignment.TopStart),
        ) {
            Badge(
                color = MaterialTheme.colorScheme.secondary,
                textColor = MaterialTheme.colorScheme.onSecondary,
                text = pluralStringResource(
                    MR.plurals.manga_num_chapters,
                    duplicate.chapterCount.toInt(),
                    duplicate.chapterCount,
                ),
            )
        }
    }
}

@Composable
internal fun StatusRow(status: Long) {
    val presentation = mangaStatusPresentation(status)
    MangaDetailRow(
        text = stringResource(presentation.label),
        iconImageVector = presentation.icon,
    )
}

@Composable
internal fun MangaDetailRow(
    text: String,
    iconImageVector: ImageVector,
    maxLines: Int = 1,
) {
    Row(
        modifier = Modifier
            .secondaryItemAlpha()
            .padding(top = MaterialTheme.padding.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconImageVector,
            contentDescription = null,
            modifier = Modifier.size(MangaDetailsIconWidth),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
        )
    }
}
