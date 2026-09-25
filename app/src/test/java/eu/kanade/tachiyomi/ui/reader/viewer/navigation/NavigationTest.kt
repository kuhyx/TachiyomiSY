package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.PointF
import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.invert
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class NavigationTest {

    private fun ViewerNavigation.at(x: Float, y: Float): NavigationRegion = getAction(PointF(x, y))

    @Test
    fun lShapedZones() {
        val nav = LNavigation()
        nav.at(0.1f, 0.5f) shouldBe NavigationRegion.PREV
        nav.at(0.5f, 0.1f) shouldBe NavigationRegion.PREV
        nav.at(0.9f, 0.5f) shouldBe NavigationRegion.NEXT
        nav.at(0.5f, 0.9f) shouldBe NavigationRegion.NEXT
        nav.at(0.5f, 0.5f) shouldBe NavigationRegion.MENU
        nav.getRegions().size shouldBe 4
    }

    @Test
    fun kindlishZones() {
        val nav = KindlishNavigation()
        nav.at(0.5f, 0.02f) shouldBe NavigationRegion.MENU
        nav.at(0.5f, 0.2f) shouldBe NavigationRegion.MENU
        nav.at(0.1f, 0.5f) shouldBe NavigationRegion.PREV
        nav.at(0.6f, 0.6f) shouldBe NavigationRegion.NEXT
    }

    @Test
    fun edgeZones() {
        val nav = EdgeNavigation()
        nav.at(0.1f, 0.1f) shouldBe NavigationRegion.NEXT
        nav.at(0.9f, 0.1f) shouldBe NavigationRegion.NEXT
        nav.at(0.5f, 0.9f) shouldBe NavigationRegion.PREV
        nav.at(0.5f, 0.5f) shouldBe NavigationRegion.MENU
    }

    @Test
    fun rightAndLeftZones() {
        val nav = RightAndLeftNavigation()
        nav.at(0.1f, 0.5f) shouldBe NavigationRegion.LEFT
        nav.at(0.9f, 0.5f) shouldBe NavigationRegion.RIGHT
        nav.at(0.5f, 0.5f) shouldBe NavigationRegion.MENU
    }

    @Test
    fun disabledIsAllMenu() {
        val nav = DisabledNavigation()
        nav.getRegions() shouldBe emptyList()
        nav.at(0.1f, 0.9f) shouldBe NavigationRegion.MENU
    }

    @Test
    fun invertedZonesSwapSides() {
        val nav = RightAndLeftNavigation()
        nav.invertMode shouldBe TappingInvertMode.NONE
        nav.invertMode = TappingInvertMode.HORIZONTAL
        nav.at(0.1f, 0.5f) shouldBe NavigationRegion.RIGHT
        val l = LNavigation()
        l.invertMode = TappingInvertMode.VERTICAL
        l.at(0.5f, 0.1f) shouldBe NavigationRegion.NEXT
        l.invertMode = TappingInvertMode.BOTH
        l.at(0.1f, 0.5f) shouldBe NavigationRegion.NEXT
    }

    @Test
    fun regionInvertKeepsIdentity() {
        val region = ViewerNavigation.Region(RectF(0f, 0f, 0.5f, 1f), NavigationRegion.PREV)
        region.invert(TappingInvertMode.NONE) shouldBeSameInstanceAs region
        region.invert(TappingInvertMode.HORIZONTAL).rectF shouldBe RectF(0.5f, 0f, 1f, 1f)
        region.copy(type = NavigationRegion.NEXT).type shouldBe NavigationRegion.NEXT
    }

    @Test
    fun regionLabelsAndColors() {
        NavigationRegion.MENU.nameRes shouldBe MR.strings.action_menu
        NavigationRegion.PREV.nameRes shouldBe MR.strings.nav_zone_prev
        NavigationRegion.NEXT.nameRes shouldBe MR.strings.nav_zone_next
        NavigationRegion.LEFT.nameRes shouldBe MR.strings.nav_zone_left
        NavigationRegion.RIGHT.nameRes shouldBe MR.strings.nav_zone_right
        listOf(
            NavigationRegion.MENU,
            NavigationRegion.PREV,
            NavigationRegion.NEXT,
            NavigationRegion.LEFT,
            NavigationRegion.RIGHT,
        ).map { it.color }.toSet().size shouldBe 5
    }
}
