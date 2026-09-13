package tachiyomi.core.common.util.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import okio.Buffer
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil.Side
import tachiyomi.decoder.ImageDecoder
import kotlin.math.max

private const val JPEG_QUALITY = 100
private const val CENTER_PADDING = 96

/** Cuts, rotates and re-joins double-page spreads. */
internal object ImageSplitting {
    /**
     * Check whether the image is wide (which we consider a double-page spread).
     *
     * @return true if the width is greater than the height
     */
    fun isWideImage(imageSource: BufferedSource): Boolean {
        val options = extractImageOptions(imageSource)
        return options.outWidth > options.outHeight
    }

    /**
     * Extract the 'side' part from [BufferedSource] and return it as [BufferedSource].
     */
    fun splitInHalf(imageSource: BufferedSource, side: Side, sidePadding: Int): BufferedSource {
        val imageBitmap = BitmapFactory.decodeStream(imageSource.inputStream())
        val height = imageBitmap.height
        val width = imageBitmap.width

        val singlePage = Rect(0, 0, width / 2 + sidePadding, height)

        val half = createBitmap(width / 2 + sidePadding, height)
        val part = when (side) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        half.applyCanvas {
            drawBitmap(imageBitmap, part, singlePage, null)
        }
        val output = Buffer()
        half.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output.outputStream())

        return output
    }

    fun rotateImage(imageSource: BufferedSource, degrees: Float): BufferedSource {
        val imageBitmap = BitmapFactory.decodeStream(imageSource.inputStream())
        val rotated = rotateBitMap(imageBitmap, degrees)

        val output = Buffer()
        rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output.outputStream())

        return output
    }

    fun rotateBitMap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Split the image into left and right parts, then merge them into a
     * new vertically-aligned image.
     */
    fun splitAndMerge(imageSource: BufferedSource, upperSide: Side): BufferedSource {
        val imageBitmap = BitmapFactory.decodeStream(imageSource.inputStream())
        val height = imageBitmap.height
        val width = imageBitmap.width

        val result = createBitmap(width / 2, height * 2)
        result.applyCanvas {
            // right -> upper
            val rightPart = when (upperSide) {
                Side.RIGHT -> Rect(width - width / 2, 0, width, height)
                Side.LEFT -> Rect(0, 0, width / 2, height)
            }
            val upperPart = Rect(0, 0, width / 2, height)
            drawBitmap(imageBitmap, rightPart, upperPart, null)
            // left -> bottom
            val leftPart = when (upperSide) {
                Side.LEFT -> Rect(width - width / 2, 0, width, height)
                Side.RIGHT -> Rect(0, 0, width / 2, height)
            }
            val bottomPart = Rect(0, height, width / 2, height * 2)
            drawBitmap(imageBitmap, leftPart, bottomPart, null)
        }

        val output = Buffer()
        result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output.outputStream())
        return output
    }

    /**
     * Split the image into left and right parts, then merge them into a
     * new image with added center padding scaled relative to the height of the display view
     * to compensate for scaling.
     */
    fun addHorizontalCenterMargin(
        imageSource: BufferedSource,
        viewHeight: Int,
        backgroundContext: Context,
    ): BufferedSource {
        val imageBitmap = ImageDecoder.newInstance(imageSource.inputStream())?.decode()!!
        val height = imageBitmap.height
        val width = imageBitmap.width

        val centerPadding = CENTER_PADDING / (max(1, viewHeight) / height).coerceAtLeast(1)

        val leftSourcePart = Rect(0, 0, width / 2, height)
        val rightSourcePart = Rect(width / 2, 0, width, height)
        val leftTargetPart = Rect(0, 0, width / 2, height)
        val rightTargetPart = Rect(width / 2 + centerPadding, 0, width + centerPadding, height)

        val bgColor = ImageBackground.chooseBackground(backgroundContext, imageSource)
        bgColor.setBounds(width / 2, 0, width / 2 + centerPadding, height)
        val result = createBitmap(width + centerPadding, height)

        result.applyCanvas {
            drawBitmap(imageBitmap, leftSourcePart, leftTargetPart, null)
            drawBitmap(imageBitmap, rightSourcePart, rightTargetPart, null)
            bgColor.draw(this)
        }

        val output = Buffer()
        result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output.outputStream())
        return output
    }
    // SY <--
}
