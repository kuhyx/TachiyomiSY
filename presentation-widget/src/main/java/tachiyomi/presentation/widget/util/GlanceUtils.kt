package tachiyomi.presentation.widget.util

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import tachiyomi.presentation.widget.R

private const val PADDED_COVER_HEIGHT_DP = 95
private const val PADDED_COVER_WIDTH_DP = 64
private const val GLANCE_MAX_CHILDREN = 10

/** Rounds the widget's outer corners by the launcher-provided radius. */
public fun GlanceModifier.appWidgetBackgroundRadius(): GlanceModifier =
    this.cornerRadius(R.dimen.appwidget_background_radius)

/** Rounds a cover's corners by the widget's inner radius. */
public fun GlanceModifier.appWidgetInnerRadius(): GlanceModifier = this.cornerRadius(R.dimen.appwidget_inner_radius)

/**
 * Calculates row-column count.
 *
 * Row
 * Numerator: Container height - container vertical padding
 * Denominator: Cover height + cover vertical padding
 *
 * Column
 * Numerator: Container width - container horizontal padding
 * Denominator: Cover width + cover horizontal padding
 *
 * @return pair of row and column count
 */
public fun DpSize.calculateRowAndColumnCount(
    topPadding: Dp,
    bottomPadding: Dp,
): Pair<Int, Int> {
    // Hack: Size provided by Glance manager is not reliable so take at least 1 row and 1 column
    // Set max to 10 children each direction because of Glance limitation
    val height = this.height - topPadding - bottomPadding
    val rowCount = (height.value / PADDED_COVER_HEIGHT_DP).toInt().coerceIn(1, GLANCE_MAX_CHILDREN)
    val columnCount = (width.value / PADDED_COVER_WIDTH_DP).toInt().coerceIn(1, GLANCE_MAX_CHILDREN)
    return Pair(rowCount, columnCount)
}

/**
 * A colour resource as a Glance day/night provider: the resource is resolved once for each
 * UI mode, so the `values-night` variant applies when the widget is drawn in the dark theme.
 * (Glance's own resource provider is restricted to its library group.)
 */
public fun Context.dayNightColorResource(@ColorRes id: Int): ColorProvider = ColorProvider(
    day = Color(inUiMode(Configuration.UI_MODE_NIGHT_NO).getColor(id)),
    night = Color(inUiMode(Configuration.UI_MODE_NIGHT_YES).getColor(id)),
)

private fun Context.inUiMode(nightMode: Int): Context {
    val configuration = Configuration(resources.configuration)
    configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
    return createConfigurationContext(configuration)
}
