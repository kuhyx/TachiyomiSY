package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Color
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.setting.centerMarginType
import eu.kanade.tachiyomi.ui.reader.setting.dualPageInvertPaged
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFit
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFitInvert
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.invertDoublePages
import eu.kanade.tachiyomi.ui.reader.setting.navigationModePager
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.pagerNavInverted
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.ZoomStartPosition
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.Preference

@RunWith(RobolectricTestRunner::class)
internal class PagerConfigTest {

    private val prefs = ReaderPreferences(MapPreferenceStore())
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(dispatcher)

    private fun config(viewer: PagerViewer = mockk<L2RPagerViewer>()) =
        PagerConfig(viewer, scope, prefs).also { dispatcher.scheduler.runCurrent() }

    private fun <T> Preference<T>.put(value: T) {
        set(value)
        dispatcher.scheduler.runCurrent()
    }

    private fun listened(config: PagerConfig): MutableList<String> {
        val calls = mutableListOf<String>()
        config.imagePropertyChangedListener = { calls += "image" }
        config.navigationModeChangedListener = { calls += "nav" }
        config.dualPageSplitChangedListener = { calls += "split:$it" }
        config.reloadChapterListener = { calls += "reload:$it" }
        return calls
    }

    @Test
    fun defaultsFromPreferences() {
        val config = config()
        config.theme shouldBe 1
        config.automaticBackground shouldBe false
        config.imageScaleType shouldBe 1
        config.imageZoomType shouldBe ZoomStartPosition.LEFT
        config.navigateToPan shouldBe true
        config.landscapeZoom shouldBe true
        config.doublePages shouldBe false
        config.autoDoublePages shouldBe true
        config.pageCanvasColor shouldBe Color.BLACK
        config.navigator.shouldBeInstanceOf<RightAndLeftNavigation>()
    }

    @Test
    fun zoomStartFollowsDirection() {
        config(mockk<R2LPagerViewer>()).imageZoomType shouldBe ZoomStartPosition.RIGHT
        config(mockk<VerticalPagerViewer>()).imageZoomType shouldBe ZoomStartPosition.CENTER
        val config = config()
        val expected = listOf(2 to ZoomStartPosition.LEFT, 3 to ZoomStartPosition.RIGHT, 4 to ZoomStartPosition.CENTER)
        for ((value, zoom) in expected) {
            prefs.zoomStart.put(value)
            config.imageZoomType shouldBe zoom
        }
    }

    @Test
    fun verticalDefaultsToL() {
        config(mockk<VerticalPagerViewer>()).navigator.shouldBeInstanceOf<LNavigation>()
    }

    @Test
    fun imageChangesNotify() {
        val config = config()
        val calls = listened(config)
        prefs.imageScaleType.put(2)
        prefs.zoomStart.put(2)
        prefs.cropBorders.put(true)
        prefs.navigateToPan.put(false)
        prefs.landscapeZoom.put(false)
        prefs.dualPageInvertPaged.put(true)
        prefs.dualPageRotateToFit.put(true)
        prefs.dualPageRotateToFitInvert.put(true)
        prefs.pageTransitionsPager.put(false)
        prefs.centerMarginType.put(PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN)
        calls.count { it == "image" } shouldBe 9
        config.imageCropBorders shouldBe true
        config.dualPageRotateToFitInvert shouldBe true
        config.usePageTransitions shouldBe false
        config.centerMarginType shouldBe PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN
    }

    @Test
    fun themeRecolours() {
        val config = config()
        val calls = listened(config)
        prefs.readerTheme.put(2)
        config.pageCanvasColor shouldBe 0x202125
        prefs.readerTheme.put(3)
        config.automaticBackground shouldBe true
        config.pageCanvasColor shouldBe Color.WHITE
        calls shouldBe listOf("image", "reload:false", "image", "reload:false")
    }

    @Test
    fun navigationFollowsPreference() {
        val config = config()
        val calls = listened(config)
        prefs.navigationModePager.put(2)
        config.navigator.shouldBeInstanceOf<KindlishNavigation>()
        prefs.pagerNavInverted.put(TappingInvertMode.BOTH)
        config.navigator.invertMode shouldBe TappingInvertMode.BOTH
        config.tappingInverted shouldBe TappingInvertMode.BOTH
        calls shouldBe listOf("nav", "nav")
    }

    @Test
    fun dualSplitNotifies() {
        val config = config()
        val calls = listened(config)
        prefs.dualPageSplitPaged.put(true)
        config.dualPageSplit shouldBe true
        calls shouldBe listOf("image", "split:true")
        prefs.invertDoublePages.put(true)
        config.invertDoublePages shouldBe false
    }

    @Test
    fun pageLayoutSwitchesDoubles() {
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES)
        val config = config()
        config.doublePages shouldBe true
        config.shiftDoublePage = true
        val calls = listened(config)
        prefs.pageLayout.put(PagerConfig.PageLayout.SINGLE_PAGE)
        config.doublePages shouldBe false
        config.shiftDoublePage shouldBe false
        prefs.pageLayout.put(PagerConfig.PageLayout.AUTOMATIC)
        config.autoDoublePages shouldBe true
        prefs.invertDoublePages.put(true)
        config.invertDoublePages shouldBe true
        calls shouldBe listOf("reload:false", "reload:false", "image")
    }

    @Test
    fun silentWithoutListeners() {
        prefs.pageLayout.put(PagerConfig.PageLayout.SINGLE_PAGE)
        val config = config()
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES)
        prefs.readerTheme.put(0)
        prefs.dualPageSplitPaged.put(true)
        prefs.pagerNavInverted.put(TappingInvertMode.VERTICAL)
        prefs.navigationModePager.put(1)
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES + 1)
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES)
        config.doublePages shouldBe false
        prefs.dualPageSplitPaged.put(false)
        prefs.pageLayout.put(PagerConfig.PageLayout.SINGLE_PAGE)
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES)
        config.doublePages shouldBe true
    }

    @Test
    fun doublePagesInitFromLayout() {
        prefs.pageLayout.put(PagerConfig.PageLayout.DOUBLE_PAGES)
        prefs.dualPageSplitPaged.put(true)
        val config = config()
        config.doublePages shouldBe false
        config.doublePages = true
        config.shiftDoublePage = true
        config.doublePages = true
        config.shiftDoublePage shouldBe true
        config.forceNavigationOverlay shouldBe true
    }
}
