package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.decoder.ImageDecoder
import kotlin.math.max

internal fun PagerPageHolder.process(page: ReaderPage, imageSource: BufferedSource): BufferedSource {
    if (viewer.config.dualPageRotateToFit) {
        return rotateDualPage(imageSource)
    }

    if (!viewer.config.dualPageSplit) {
        return imageSource
    }

    if (page is InsertPage) {
        return splitInHalf(imageSource)
    }

    val isDoublePage = ImageUtil.isWideImage(imageSource)
    if (!isDoublePage) {
        return imageSource
    }

    onPageSplit(page)

    return splitInHalf(imageSource)
}

internal fun PagerPageHolder.rotateDualPage(imageSource: BufferedSource): BufferedSource {
    val isDoublePage = ImageUtil.isWideImage(imageSource)
    return if (isDoublePage) {
        val rotation = if (viewer.config.dualPageRotateToFitInvert) -QUARTER_TURN_DEGREES else QUARTER_TURN_DEGREES
        ImageUtil.rotateImage(imageSource, rotation)
    } else {
        imageSource
    }
}

internal fun PagerPageHolder.handleWideImage(imageSource: BufferedSource): BufferedSource {
    val wantsCenterMargin =
        viewer.config.centerMarginType and PagerConfig.CenterMarginType.WIDE_PAGE_CENTER_MARGIN > 0 &&
            !viewer.config.imageCropBorders
    val isStillWideImage = !ImageUtil.isAnimatedAndSupported(imageSource) && ImageUtil.isWideImage(imageSource)
    return if (wantsCenterMargin && isStillWideImage) {
        ImageUtil.addHorizontalCenterMargin(imageSource, height, context)
    } else {
        imageSource
    }
}

internal fun PagerPageHolder.decodeImage(imageSource: BufferedSource): Bitmap? {
    return try {
        ImageDecoder.newInstance(imageSource.inputStream())?.decode()
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Cannot decode image" }
        null
    }
}

internal fun PagerPageHolder.calculateCenterMargin(height: Int, height2: Int): Int {
    return if (viewer.config.centerMarginType and PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN > 0 &&
        !viewer.config.imageCropBorders
    ) {
        CENTER_MARGIN_PX / (this.height.coerceAtLeast(1) / max(height, height2).coerceAtLeast(1)).coerceAtLeast(1)
    } else {
        0
    }
}

internal fun PagerPageHolder.splitDoublePages() {
    scope.launch {
        delay(PAGE_SPLIT_DELAY_MS)
        viewer.splitDoublePages(page)
        if (extraPage?.fullPage == true || page.fullPage) {
            extraPage = null
        }
    }
}

internal fun PagerPageHolder.splitInHalf(imageSource: BufferedSource): BufferedSource {
    // The inserted page shows the half the reading direction reaches second; the original the first.
    val readsLeftToRight = viewer is L2RPagerViewer
    val isInsertedHalf = page is InsertPage
    val naturalSide = if (readsLeftToRight == isInsertedHalf) ImageUtil.Side.RIGHT else ImageUtil.Side.LEFT
    val side = if (viewer.config.dualPageInvert) naturalSide.flipped() else naturalSide

    val wantsCenterMargin =
        viewer.config.centerMarginType and PagerConfig.CenterMarginType.DOUBLE_PAGE_CENTER_MARGIN > 0 &&
            viewer.config.doublePages &&
            !viewer.config.imageCropBorders
    val sideMargin = if (wantsCenterMargin) HALF_CENTER_MARGIN_PX else 0

    return ImageUtil.splitInHalf(imageSource, side, sideMargin)
}

private fun ImageUtil.Side.flipped(): ImageUtil.Side = when (this) {
    ImageUtil.Side.RIGHT -> ImageUtil.Side.LEFT
    ImageUtil.Side.LEFT -> ImageUtil.Side.RIGHT
}

internal fun PagerPageHolder.onPageSplit(page: ReaderPage) {
    val newPage = InsertPage(page)
    viewer.onPageSplit(page, newPage)
}
