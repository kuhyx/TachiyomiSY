package tachiyomi.presentation.widget.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import tachiyomi.presentation.widget.R
import tachiyomi.presentation.widget.util.appWidgetInnerRadius

/** The width every cover in the grid is drawn at. */
public val CoverWidth: Dp = 58.dp

/** The height every cover in the grid is drawn at. */
public val CoverHeight: Dp = 87.dp

/** One cover of the grid, or the placeholder when its bitmap could not be loaded. */
@Composable
public fun UpdatesMangaCover(
    cover: Bitmap?,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier
            .size(width = CoverWidth, height = CoverHeight)
            .appWidgetInnerRadius(),
    ) {
        if (cover != null) {
            Image(
                provider = ImageProvider(cover),
                contentDescription = null,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetInnerRadius(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Enjoy placeholder
            Image(
                provider = ImageProvider(R.drawable.appwidget_cover_error),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
