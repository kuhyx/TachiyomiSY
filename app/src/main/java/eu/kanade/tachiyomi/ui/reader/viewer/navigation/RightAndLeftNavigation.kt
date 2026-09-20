package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ONE_THIRD
import eu.kanade.tachiyomi.ui.reader.viewer.TWO_THIRDS
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | N | M | P |   P: Move Right
 * +---+---+---+
 * | N | M | P |   M: Menu
 * +---+---+---+
 * | N | M | P |   N: Move Left
 * +---+---+---+.
 */
internal class RightAndLeftNavigation : ViewerNavigation() {

    override var regionList: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0f, ONE_THIRD, 1f),
            type = NavigationRegion.LEFT,
        ),
        Region(
            rectF = RectF(TWO_THIRDS, 0f, 1f, 1f),
            type = NavigationRegion.RIGHT,
        ),
    )
}
