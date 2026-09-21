package eu.kanade.presentation.track

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val UNSET_TEXT_ALPHA = 0.5F

// Status / chapters / score on the first row; start and end dates below when the tracker supports them.
@Composable
internal fun TrackDetailsGrid(
    status: StringResource?,
    onStatusClick: () -> Unit,
    chapters: String,
    onChaptersClick: () -> Unit,
    score: String?,
    onScoreClick: (() -> Unit)?,
    startDate: String?,
    onStartDateClick: (() -> Unit)?,
    endDate: String?,
    onEndDateClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(8.dp)
            .clip(RoundedCornerShape(6.dp)),
    ) {
        Column {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                TrackDetailsItem(
                    modifier = Modifier.weight(1f),
                    text = status?.let { stringResource(it) } ?: "",
                    onClick = onStatusClick,
                )
                VerticalDivider()
                TrackDetailsItem(
                    modifier = Modifier.weight(1f),
                    text = chapters,
                    onClick = onChaptersClick,
                )
                if (onScoreClick != null) {
                    VerticalDivider()
                    TrackDetailsItem(
                        modifier = Modifier.weight(1f),
                        text = score,
                        placeholder = stringResource(MR.strings.score),
                        onClick = onScoreClick,
                    )
                }
            }

            if (onStartDateClick != null && onEndDateClick != null) {
                HorizontalDivider()
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    TrackDetailsItem(
                        modifier = Modifier.weight(1F),
                        text = startDate,
                        placeholder = stringResource(MR.strings.track_started_reading_date),
                        onClick = onStartDateClick,
                    )
                    VerticalDivider()
                    TrackDetailsItem(
                        modifier = Modifier.weight(1F),
                        text = endDate,
                        placeholder = stringResource(MR.strings.track_finished_reading_date),
                        onClick = onEndDateClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TrackDetailsItem(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxHeight()
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text ?: placeholder,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (text == null) UNSET_TEXT_ALPHA else 1f),
        )
    }
}
