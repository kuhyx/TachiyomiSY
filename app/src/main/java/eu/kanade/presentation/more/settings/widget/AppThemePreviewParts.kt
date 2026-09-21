package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

private const val PREVIEW_TITLE_HEIGHT = 0.8f
private const val PREVIEW_TITLE_WEIGHT = 0.7f
private const val PREVIEW_ACTION_WEIGHT = 0.3f
private const val PREVIEW_COVER_WIDTH = 0.5f
private const val PREVIEW_SUBTITLE_ALPHA = 0.6f

@Composable
internal fun PreviewAppBar(selected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(PREVIEW_TITLE_HEIGHT)
                .weight(PREVIEW_TITLE_WEIGHT)
                .padding(end = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.small,
                ),
        )

        Box(
            modifier = Modifier.weight(PREVIEW_ACTION_WEIGHT),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(MR.strings.selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun PreviewCover() {
    Box(
        modifier = Modifier
            .padding(start = 8.dp, top = 2.dp)
            .background(
                color = DividerDefaults.color,
                shape = MaterialTheme.shapes.small,
            )
            .fillMaxWidth(PREVIEW_COVER_WIDTH)
            .aspectRatio(MangaCover.Book.ratio),
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .size(width = 24.dp, height = 16.dp)
                .clip(RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

@Composable
internal fun ColumnScope.PreviewBottomBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .alpha(PREVIEW_SUBTITLE_ALPHA)
                        .height(17.dp)
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.small,
                        ),
                )
            }
        }
    }
}
