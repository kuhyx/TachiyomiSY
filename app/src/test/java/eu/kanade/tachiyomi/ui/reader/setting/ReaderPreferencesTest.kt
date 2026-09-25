package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ArchiveReaderMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.FlashColor
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.ReaderHideThreshold
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class ReaderPreferencesTest {

    private val prefs = ReaderPreferences(InMemoryPreferenceStore())

    @Test
    fun generalDefaults() {
        prefs.pageTransitionsPager.get() shouldBe true
        prefs.pageTransitionsWebtoon.get() shouldBe true
        prefs.flashOnPageChange.get() shouldBe false
        prefs.flashDurationMillis.get() shouldBe ReaderPreferences.MILLI_CONVERSION
        prefs.flashPageInterval.get() shouldBe 1
        prefs.flashColor.get() shouldBe FlashColor.BLACK
        prefs.doubleTapAnimSpeed.get() shouldBe 500
        prefs.showPageNumber.get() shouldBe true
        prefs.verticalNavigator.get() shouldBe emptySet()
        prefs.verticalNavigatorOnLeft.get() shouldBe false
        prefs.verticalNavigatorHeight.get() shouldBe 65
        prefs.showReadingMode.get() shouldBe true
        prefs.fullscreen.get() shouldBe true
        prefs.drawUnderCutout.get() shouldBe true
        prefs.keepScreenOn.get() shouldBe false
        prefs.defaultReadingMode.get() shouldBe ReadingMode.RIGHT_TO_LEFT.flagValue
        prefs.defaultOrientationType.get() shouldBe ReaderOrientation.FREE.flagValue
    }

    @Test
    fun viewerDefaults() {
        prefs.webtoonDoubleTapZoomEnabled.get() shouldBe true
        prefs.imageScaleType.get() shouldBe 1
        prefs.zoomStart.get() shouldBe 1
        prefs.readerTheme.get() shouldBe 1
        prefs.alwaysShowChapterTransition.get() shouldBe true
        prefs.cropBorders.get() shouldBe false
        prefs.navigateToPan.get() shouldBe true
        prefs.landscapeZoom.get() shouldBe true
        prefs.cropBordersWebtoon.get() shouldBe false
        prefs.webtoonSidePadding.get() shouldBe ReaderPreferences.WEBTOON_PADDING_MIN
        prefs.readerHideThreshold.get() shouldBe ReaderHideThreshold.LOW
        prefs.folderPerManga.get() shouldBe false
        prefs.skipRead.get() shouldBe false
        prefs.skipFiltered.get() shouldBe true
        prefs.skipDupe.get() shouldBe false
        prefs.webtoonDisableZoomOut.get() shouldBe false
    }

    @Test
    fun colorFilterDefaults() {
        prefs.customBrightness.get() shouldBe false
        prefs.customBrightnessValue.get() shouldBe 0
        prefs.colorFilter.get() shouldBe false
        prefs.colorFilterValue.get() shouldBe 0
        prefs.colorFilterMode.get() shouldBe 0
        prefs.grayscale.get() shouldBe false
        prefs.invertedColors.get() shouldBe false
    }

    @Test
    fun controlDefaults() {
        prefs.readWithLongTap.get() shouldBe true
        prefs.readWithVolumeKeys.get() shouldBe false
        prefs.readWithVolumeKeysInverted.get() shouldBe false
        prefs.navigationModePager.get() shouldBe 0
        prefs.navigationModeWebtoon.get() shouldBe 0
        prefs.pagerNavInverted.get() shouldBe TappingInvertMode.NONE
        prefs.webtoonNavInverted.get() shouldBe TappingInvertMode.NONE
        prefs.showNavigationOverlayNewUser.get() shouldBe true
        prefs.showNavigationOverlayOnStart.get() shouldBe false
    }

    @Test
    fun dualPageDefaults() {
        prefs.dualPageSplitPaged.get() shouldBe false
        prefs.dualPageInvertPaged.get() shouldBe false
        prefs.dualPageSplitWebtoon.get() shouldBe false
        prefs.dualPageInvertWebtoon.get() shouldBe false
        prefs.dualPageRotateToFit.get() shouldBe false
        prefs.dualPageRotateToFitInvert.get() shouldBe false
        prefs.dualPageRotateToFitWebtoon.get() shouldBe false
        prefs.dualPageRotateToFitInvertWebtoon.get() shouldBe false
    }

    @Test
    fun syDefaults() {
        prefs.readerThreads.get() shouldBe 2
        prefs.readerInstantRetry.get() shouldBe true
        prefs.aggressivePageLoading.get() shouldBe false
        prefs.cacheSize.get() shouldBe "75"
        prefs.autoscrollInterval.get() shouldBe 3f
        prefs.smoothAutoScroll.get() shouldBe true
        prefs.preserveReadingPosition.get() shouldBe false
        prefs.preloadSize.get() shouldBe 10
        prefs.useAutoWebtoon.get() shouldBe true
        prefs.continuousVerticalTappingByPage.get() shouldBe false
        prefs.cropBordersContinuousVertical.get() shouldBe false
        prefs.readerBottomButtons.get() shouldBe ReaderBottomButton.BUTTONS_DEFAULTS
        prefs.pageLayout.get() shouldBe PagerConfig.PageLayout.AUTOMATIC
        prefs.invertDoublePages.get() shouldBe false
        prefs.centerMarginType.get() shouldBe PagerConfig.CenterMarginType.NONE
        prefs.archiveReaderMode.get() shouldBe ArchiveReaderMode.LOAD_FROM_FILE
    }

    @Test
    fun nestedEnumsAndConstants() {
        FlashColor.entries.size shouldBe 3
        TappingInvertMode.NONE.shouldInvertHorizontal shouldBe false
        TappingInvertMode.NONE.shouldInvertVertical shouldBe false
        TappingInvertMode.BOTH.shouldInvertHorizontal shouldBe true
        TappingInvertMode.VERTICAL.shouldInvertVertical shouldBe true
        ReaderHideThreshold.HIGHEST.threshold shouldBe 5
        ArchiveReaderMode.LOAD_INTO_MEMORY shouldBe 1
        ArchiveReaderMode.CACHE_TO_DISK shouldBe 2
        ReaderPreferences.WEBTOON_PADDING_MAX shouldBe 25
    }

    @Test
    fun companionLists() {
        ReaderPreferences.TapZones.size shouldBe 6
        ReaderPreferences.ImageScaleType.size shouldBe 6
        ReaderPreferences.ZoomStart.size shouldBe 4
        ReaderPreferences.PageLayouts.size shouldBe 3
        ReaderPreferences.CenterMarginTypes.size shouldBe 4
        ReaderPreferences.archiveModeTypes.size shouldBe 3
        prefs.preferenceStore.getInt("page_layout", -1).get() shouldBe -1
    }
}
