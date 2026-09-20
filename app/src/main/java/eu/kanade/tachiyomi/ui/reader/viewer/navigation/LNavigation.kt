package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ONE_THIRD
import eu.kanade.tachiyomi.ui.reader.viewer.TWO_THIRDS
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion
 * +---+---+---+
 * | P | P | P |   P: Previous
 * +---+---+---+
 * | P | M | N |   M: Menu
 * +---+---+---+
 * | N | N | N |   N: Next
 * +---+---+---+.
 */
internal open class LNavigation : ViewerNavigation() {

    override var regionList: List<Region> = listOf(
        Region(
            rectF = RectF(0f, ONE_THIRD, ONE_THIRD, TWO_THIRDS),
            type = NavigationRegion.PREV,
        ),
        Region(
            rectF = RectF(0f, 0f, 1f, ONE_THIRD),
            type = NavigationRegion.PREV,
        ),
        Region(
            rectF = RectF(TWO_THIRDS, ONE_THIRD, 1f, TWO_THIRDS),
            type = NavigationRegion.NEXT,
        ),
        Region(
            rectF = RectF(0f, TWO_THIRDS, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
    )
}
