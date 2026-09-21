package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.graphics.Bitmap
import kotlinx.coroutines.launch
import logcat.LogPriority
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat

// Which of the two pages of a spread rules the merge out.
private enum class Unmergeable {
    FIRST,
    SECOND,
}

internal fun PagerPageHolder.mergePages(imageSource: BufferedSource, imageSource2: BufferedSource?): BufferedSource {
    // Handle adding a center margin to wide images if requested
    if (imageSource2 == null) return handleWideImage(imageSource)
    if (page.fullPage) return imageSource
    if (keepApartIfAnimated(imageSource, imageSource2)) return imageSource
    val imageBitmap = mergeableBitmap(imageSource, imageSource2, Unmergeable.FIRST, PROGRESS_SPLITTING)
        ?: return imageSource
    val imageBitmap2 = mergeableBitmap(imageSource2, imageSource2, Unmergeable.SECOND, PROGRESS_MERGING)
        ?: return imageSource

    val isLTR = (viewer !is R2LPagerViewer) xor viewer.config.invertDoublePages
    val centerMargin = calculateCenterMargin(imageBitmap.height, imageBitmap2.height)

    imageSource.close()
    imageSource2.close()

    return ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, centerMargin, viewer.config.pageCanvasColor) {
        updateProgress(it)
    }
}

// Animations cannot be merged: true after arranging for the animated page to show on its own.
private fun PagerPageHolder.keepApartIfAnimated(imageSource: BufferedSource, imageSource2: BufferedSource): Boolean {
    val animated = when {
        ImageUtil.isAnimatedAndSupported(imageSource) -> Unmergeable.FIRST
        ImageUtil.isAnimatedAndSupported(imageSource2) -> Unmergeable.SECOND
        else -> return false
    }
    keepApart(animated, log = false)
    return true
}

// A page decoded for merging, or null (with the pages arranged to show apart and the second source closed)
// when it cannot be decoded or is already a landscape spread.
private fun PagerPageHolder.mergeableBitmap(
    source: BufferedSource,
    imageSource2: BufferedSource,
    which: Unmergeable,
    progress: Int,
): Bitmap? {
    val bitmap = decodeImage(source)
    if (bitmap != null) {
        scope.launch { progressIndicator?.setProgress(progress) }
    }
    val portrait = bitmap != null && bitmap.height >= bitmap.width
    if (!portrait) {
        imageSource2.close()
        keepApart(which, log = bitmap == null)
        return null
    }
    return bitmap
}

private fun PagerPageHolder.keepApart(which: Unmergeable, log: Boolean) {
    when (which) {
        Unmergeable.FIRST -> {
            page.fullPage = true
        }
        Unmergeable.SECOND -> {
            page.isolatedPage = true
            extraPage?.fullPage = true
        }
    }
    splitDoublePages()
    if (log) logcat(LogPriority.ERROR) { "Cannot combine pages" }
}
