package tachiyomi.core.common.util.system

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.hippo.unifile.UniFile
import okio.BufferedSource
import java.io.File
import java.io.InputStream
import kotlin.math.max

/**
 * Image helpers used by the reader, the downloader and the library. Every member keeps its
 * historical name; the work lives in the internal objects of this package, one concern each.
 */
public object ImageUtil {

    /** True when [name] has an image extension, or [openStream] yields a recognisable image. */
    public fun isImage(name: String?, openStream: (() -> InputStream)? = null): Boolean =
        ImageTypeDetection.isImage(name, openStream)

    /** The image type behind [openStream], or null when it is not a supported image. */
    public fun findImageType(openStream: () -> InputStream): ImageType? = ImageTypeDetection.findImageType(openStream)

    /** The image type of [stream], or null when it is not a supported image. */
    public fun findImageType(stream: InputStream): ImageType? = ImageTypeDetection.findImageType(stream)

    /** The file extension for [mime], sniffing [openStream] when the mime type is unknown; `jpg` by default. */
    public fun getExtensionFromMimeType(mime: String?, openStream: () -> InputStream): String =
        ImageTypeDetection.getExtensionFromMimeType(mime, openStream)

    /** True when [source] is an animated image this Android version can play. */
    public fun isAnimatedAndSupported(source: BufferedSource): Boolean = ImageTypeDetection.isAnimatedAndSupported(source)

    /** True if the width is greater than the height, which we consider a double-page spread. */
    public fun isWideImage(imageSource: BufferedSource): Boolean = ImageSplitting.isWideImage(imageSource)

    /** Extract the [side] half of [imageSource], with [sidePadding] extra pixels past the middle. */
    public fun splitInHalf(imageSource: BufferedSource, side: Side, sidePadding: Int): BufferedSource =
        ImageSplitting.splitInHalf(imageSource, side, sidePadding)

    /** [imageSource] rotated by [degrees]. */
    public fun rotateImage(imageSource: BufferedSource, degrees: Float): BufferedSource =
        ImageSplitting.rotateImage(imageSource, degrees)

    /** Split the image into left and right parts, then merge them into a new vertically-aligned image. */
    public fun splitAndMerge(imageSource: BufferedSource, upperSide: Side): BufferedSource =
        ImageSplitting.splitAndMerge(imageSource, upperSide)

    // SY -->

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

    /** True when [bitmap] fits the hardware texture limit on a device that supports hardware bitmaps. */
    public fun canUseHardwareBitmap(bitmap: Bitmap): Boolean = HardwareBitmaps.canUseHardwareBitmap(bitmap)

    /** True when the image in [imageSource] fits the hardware texture limit on a supported device. */
    public fun canUseHardwareBitmap(imageSource: BufferedSource): Boolean = HardwareBitmaps.canUseHardwareBitmap(imageSource)

    /** Largest dimension allowed for a hardware bitmap; the reader lowers it on GL errors. */
    public var hardwareBitmapThreshold: Int
        get() = HardwareBitmaps.hardwareBitmapThreshold
        set(value) {
            HardwareBitmaps.hardwareBitmapThreshold = value
        }

    /** Devices whose hardware bitmap implementation is known to be broken (list taken from Coil). */
    public val HARDWARE_BITMAP_UNSUPPORTED: Boolean
        get() = HardwareBitmaps.HARDWARE_BITMAP_UNSUPPORTED

    /** Algorithm for determining what background to accompany a comic/manga page. */
    public fun chooseBackground(context: Context, imageSource: BufferedSource): Drawable =
        ImageBackground.chooseBackground(context, imageSource)

    // SY -->

    /** Writes random EXIF padding so files inside CBZ archives get unique sizes. */
    public fun addPaddingToImageExif(imageFile: File) {
        ImageExifPadding.addPaddingToImageExif(imageFile)
    }

    /** Two pages side by side, [isLTR] deciding which goes left, with [centerMargin] of [background] between. */
    public fun mergeBitmaps(
        imageBitmap: Bitmap,
        imageBitmap2: Bitmap,
        isLTR: Boolean,
        centerMargin: Int,
        @ColorInt background: Int = Color.WHITE,
        progressCallback: ((Int) -> Unit)? = null,
    ): BufferedSource = BitmapMerging.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, centerMargin, background, progressCallback)
    // SY <--

    public enum class ImageType(public val mime: String, public val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    public enum class Side {
        RIGHT,
        LEFT,
    }
    // SY -->

    public data class SplitData(
        val index: Int,
        val topOffset: Int,
        val splitHeight: Int,
        val splitWidth: Int,
    ) {
        public val bottomOffset: Int = topOffset + splitHeight
    }
}

public val getDisplayMaxHeightInPx: Int
    get() = Resources.getSystem().displayMetrics.let { max(it.heightPixels, it.widthPixels) }
