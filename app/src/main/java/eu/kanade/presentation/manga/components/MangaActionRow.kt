package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
internal fun MangaActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    // SY -->
    onMergeClicked: (() -> Unit)?,
    // SY <--
    modifier: Modifier = Modifier,
) {
    val defaultActionButtonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)

    // Follow-up: show something better when using custom interval (https://github.com/kuhyx/TachiyomiSY/issues/12)
    val nextUpdateDays = remember(nextUpdate) {
        nextUpdate?.let { Instant.now().until(it, ChronoUnit.DAYS).toInt().coerceAtLeast(0) }
    }

    Row(modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)) {
        FavoriteButton(
            favorite = favorite,
            defaultColor = defaultActionButtonColor,
            onClick = onAddToLibraryClicked,
            onLongClick = onEditCategory,
        )
        MangaActionButton(
            title = nextUpdateTitle(nextUpdateDays),
            icon = Icons.Default.HourglassEmpty,
            color = if (isUserIntervalMode) MaterialTheme.colorScheme.primary else defaultActionButtonColor,
            onClick = { onEditIntervalClicked?.invoke() },
        )
        TrackingButton(
            trackingCount = trackingCount,
            defaultColor = defaultActionButtonColor,
            onClick = onTrackingClicked,
        )
        if (onWebViewClicked != null) {
            MangaActionButton(
                title = stringResource(MR.strings.action_web_view),
                icon = Icons.Outlined.Public,
                color = defaultActionButtonColor,
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
        // SY -->
        if (onMergeClicked != null) {
            MangaActionButton(
                title = stringResource(SYMR.strings.merge),
                icon = Icons.AutoMirrored.Outlined.CallMerge,
                color = defaultActionButtonColor,
                onClick = onMergeClicked,
            )
        }
        // SY <--
    }
}

@Composable
internal fun RowScope.FavoriteButton(
    favorite: Boolean,
    defaultColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    MangaActionButton(
        title = if (favorite) {
            stringResource(MR.strings.in_library)
        } else {
            stringResource(MR.strings.add_to_library)
        },
        icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        color = if (favorite) MaterialTheme.colorScheme.primary else defaultColor,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
internal fun RowScope.TrackingButton(trackingCount: Int, defaultColor: Color, onClick: () -> Unit) {
    MangaActionButton(
        title = if (trackingCount == 0) {
            stringResource(MR.strings.manga_tracking_tab)
        } else {
            pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
        },
        icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
        color = if (trackingCount == 0) defaultColor else MaterialTheme.colorScheme.primary,
        onClick = onClick,
    )
}

@Composable
internal fun nextUpdateTitle(nextUpdateDays: Int?): String = when (nextUpdateDays) {
    null -> stringResource(MR.strings.not_applicable)
    0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
    else -> pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays)
}

@Composable
internal fun RowScope.MangaActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        onLongClick = onLongClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
