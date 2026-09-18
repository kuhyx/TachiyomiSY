package tachiyomi.core.common.util.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import com.hippo.unifile.UniFile
import okio.BufferedSource

/** The splitting, rotating and merging half of [ImageUtil]; a layer so each class stays small. */
public abstract class ImageUtilSplitting {
    /** True if the width is greater than the height, which we consider a double-page spread. */
    public fun isWideImage(imageSource: BufferedSource): Boolean = ImageSplitting.isWideImage(imageSource)

    /** Extract the [side] half of [imageSource], with [sidePadding] extra pixels past the middle. */
    public fun splitInHalf(imageSource: BufferedSource, side: ImageUtil.Side, sidePadding: Int): BufferedSource =
        ImageSplitting.splitInHalf(imageSource, side, sidePadding)

    /** [imageSource] rotated by [degrees]. */
    public fun rotateImage(imageSource: BufferedSource, degrees: Float): BufferedSource =
        ImageSplitting.rotateImage(imageSource, degrees)

    /** Split the image into left and right parts, then merge them into a new vertically-aligned image. */
    public fun splitAndMerge(imageSource: BufferedSource, upperSide: ImageUtil.Side): BufferedSource =
        ImageSplitting.splitAndMerge(imageSource, upperSide)

    /** The spread with a centre margin, scaled to [viewHeight], filled with the page background. */
    public fun addHorizontalCenterMargin(
        imageSource: BufferedSource,
        viewHeight: Int,
        backgroundContext: Context,
    ): BufferedSource = ImageSplitting.addHorizontalCenterMargin(imageSource, viewHeight, backgroundContext)
    // SY <--

    /** Splits tall images to improve performance of reader; true when nothing was left to do or all parts landed. */
    public fun splitTallImage(tmpDir: UniFile, imageFile: UniFile, filenamePrefix: String): Boolean =
        TallImageSplitting.splitTallImage(tmpDir, imageFile, filenamePrefix)

    /** Two pages side by side, [isLTR] deciding which goes left, with [centerMargin] of [background] between. */
    public fun mergeBitmaps(
        imageBitmap: Bitmap,
        imageBitmap2: Bitmap,
        isLTR: Boolean,
        centerMargin: Int,
        @ColorInt background: Int = Color.WHITE,
        progressCallback: ((Int) -> Unit)? = null,
    ): BufferedSource =
        BitmapMerging.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, centerMargin, background, progressCallback)
    // SY <--
}
