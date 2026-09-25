package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.setting.cropBordersContinuousVertical
import eu.kanade.tachiyomi.ui.reader.setting.dualPageInvertWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFitInvertWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFitWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.navigationModeWebtoon
import eu.kanade.tachiyomi.ui.reader.setting.readWithLongTap
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeys
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeysInverted
import eu.kanade.tachiyomi.ui.reader.setting.showNavigationOverlayOnStart
import eu.kanade.tachiyomi.ui.reader.setting.webtoonNavInverted
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.Preference

@RunWith(RobolectricTestRunner::class)
internal class WebtoonConfigTest {

    private val prefs = ReaderPreferences(MapPreferenceStore())
    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(dispatcher)

    private fun config() = WebtoonConfig(scope, prefs).also { dispatcher.scheduler.runCurrent() }

    private fun <T> Preference<T>.put(value: T) {
        set(value)
        dispatcher.scheduler.runCurrent()
    }

    private fun listened(config: WebtoonConfig): MutableList<String> {
        val calls = mutableListOf<String>()
        config.imagePropertyChangedListener = { calls += "image" }
        config.navigationModeChangedListener = { calls += "nav" }
        config.themeChangedListener = { calls += "theme" }
        config.zoomPropertyChangedListener = { calls += "zoom:$it" }
        config.doubleTapZoomChangedListener = { calls += "tap:$it" }
        return calls
    }

    @Test
    fun defaultsFromPreferences() {
        val config = config()
        config.theme shouldBe 1
        config.imageCropBorders shouldBe false
        config.zoomOutDisabled shouldBe false
        config.sidePadding shouldBe 0
        config.doubleTapZoom shouldBe true
        config.usePageTransitions shouldBe true
        config.continuousCropBorders shouldBe false
        config.navigator.shouldBeInstanceOf<LNavigation>()
        config.forceNavigationOverlay shouldBe true
        config().forceNavigationOverlay shouldBe false
    }

    @Test
    fun imageChangesNotify() {
        val config = config()
        val calls = listened(config)
        prefs.cropBordersWebtoon.put(true)
        prefs.webtoonSidePadding.put(5)
        prefs.dualPageSplitWebtoon.put(true)
        prefs.dualPageInvertWebtoon.put(true)
        prefs.dualPageRotateToFitWebtoon.put(true)
        prefs.dualPageRotateToFitInvertWebtoon.put(true)
        prefs.cropBordersContinuousVertical.put(true)
        prefs.pageTransitionsWebtoon.put(false)
        calls.count { it == "image" } shouldBe 8
        config.sidePadding shouldBe 5
        config.dualPageSplit shouldBe true
        config.dualPageInvert shouldBe true
        config.dualPageRotateToFit shouldBe true
        config.dualPageRotateToFitInvert shouldBe true
    }

    @Test
    fun zoomAndThemeNotify() {
        val config = config()
        val calls = listened(config)
        prefs.webtoonDisableZoomOut.put(true)
        prefs.webtoonDoubleTapZoomEnabled.put(false)
        prefs.readerTheme.put(2)
        prefs.readerTheme.put(2)
        calls shouldBe listOf("zoom:true", "tap:false", "theme")
    }

    @Test
    fun everyNavigationMode() {
        val config = config()
        val calls = listened(config)
        val expected = listOf(
            1 to LNavigation::class,
            2 to KindlishNavigation::class,
            3 to EdgeNavigation::class,
            4 to RightAndLeftNavigation::class,
            5 to DisabledNavigation::class,
            9 to LNavigation::class,
        )
        for ((mode, type) in expected) {
            prefs.navigationModeWebtoon.put(mode)
            config.navigator::class shouldBe type
        }
        config.navigationMode shouldBe 9
        calls.size shouldBe 6
    }

    @Test
    fun invertedTapsNotify() {
        val config = config()
        val calls = listened(config)
        prefs.webtoonNavInverted.put(TappingInvertMode.HORIZONTAL)
        config.navigator.invertMode shouldBe TappingInvertMode.HORIZONTAL
        prefs.navigationModeWebtoon.put(2)
        config.navigator.invertMode shouldBe TappingInvertMode.HORIZONTAL
        calls shouldBe listOf("nav", "nav")
    }

    @Test
    fun silentWithoutListeners() {
        val config = config()
        prefs.cropBordersWebtoon.put(true)
        prefs.webtoonNavInverted.put(TappingInvertMode.BOTH)
        prefs.webtoonDisableZoomOut.put(true)
        prefs.webtoonDoubleTapZoomEnabled.put(false)
        prefs.readerTheme.put(3)
        prefs.navigationModeWebtoon.put(1)
        config.imageCropBorders shouldBe true
    }

    @Test
    fun commonPreferencesTracked() {
        val config = config()
        prefs.readWithLongTap.put(false)
        prefs.doubleTapAnimSpeed.put(250)
        prefs.readWithVolumeKeys.put(true)
        prefs.readWithVolumeKeysInverted.put(true)
        prefs.alwaysShowChapterTransition.put(false)
        prefs.showNavigationOverlayOnStart.put(true)
        config.longTapEnabled shouldBe false
        config.doubleTapAnimDuration shouldBe 250
        config.volumeKeysEnabled shouldBe true
        config.volumeKeysInverted shouldBe true
        config.alwaysShowChapterTransition shouldBe false
        config.navigationOverlayOnStart shouldBe true
    }

    @Test
    fun preferencesFromInjekt() {
        startKoin { modules(module { single { prefs } }) }
        try {
            WebtoonConfig(scope).theme shouldBe 1
            PagerConfig(mockk<L2RPagerViewer>(), scope).theme shouldBe 1
        } finally {
            stopKoin()
        }
    }
}
