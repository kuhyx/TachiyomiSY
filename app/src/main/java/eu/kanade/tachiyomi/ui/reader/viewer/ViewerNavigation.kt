package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.lang.invert
import tachiyomi.i18n.MR

/** The strip along the top that always opens the menu, as a fraction of the screen height. */
private const val MENU_REGION_HEIGHT = 0.05f

/** The tap layouts split the screen into thirds; these are the cut points as screen fractions. */
internal const val ONE_THIRD = 0.33f
internal const val TWO_THIRDS = 0.66f

internal abstract class ViewerNavigation {

    sealed class NavigationRegion(val nameRes: StringResource, val color: Int) {
        data object MENU : NavigationRegion(MR.strings.action_menu, MENU_COLOR)
        data object PREV : NavigationRegion(MR.strings.nav_zone_prev, PREV_COLOR)
        data object NEXT : NavigationRegion(MR.strings.nav_zone_next, NEXT_COLOR)
        data object LEFT : NavigationRegion(MR.strings.nav_zone_left, LEFT_COLOR)
        data object RIGHT : NavigationRegion(MR.strings.nav_zone_right, RIGHT_COLOR)

        // The overlay tints of the tap-zone preview, 80% opaque.
        private companion object {
            val MENU_COLOR = Color.argb(0xCC, 0x95, 0x81, 0x8D)
            val PREV_COLOR = Color.argb(0xCC, 0xFF, 0x77, 0x33)
            val NEXT_COLOR = Color.argb(0xCC, 0x84, 0xE2, 0x96)
            val LEFT_COLOR = Color.argb(0xCC, 0x7D, 0x11, 0x28)
            val RIGHT_COLOR = Color.argb(0xCC, 0xA6, 0xCF, 0xD5)
        }
    }

    data class Region(
        val rectF: RectF,
        val type: NavigationRegion,
    ) {
        fun invert(invertMode: ReaderPreferences.TappingInvertMode): Region {
            if (invertMode == ReaderPreferences.TappingInvertMode.NONE) return this
            return this.copy(
                rectF = this.rectF.invert(invertMode),
            )
        }
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, MENU_REGION_HEIGHT)

    var invertMode: ReaderPreferences.TappingInvertMode = ReaderPreferences.TappingInvertMode.NONE

    protected abstract var regionList: List<Region>

    /** Returns regions with applied inversion. */
    fun getRegions(): List<Region> = regionList.map { it.invert(invertMode) }

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = getRegions().find { it.rectF.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }
}
