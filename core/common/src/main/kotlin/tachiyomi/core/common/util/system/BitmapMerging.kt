package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import okio.Buffer
import okio.BufferedSource
import kotlin.math.max

private const val PROGRESS_FIRST_PAGE = 98
private const val PROGRESS_SECOND_PAGE = 99
private const val PROGRESS_DONE = 100
private const val JPEG_QUALITY = 100

/** Joins two pages side by side into one image. */
internal object BitmapMerging {
    val Bitmap.rect: Rect
        get() = Rect(0, 0, width, height)
    // SY <--

    // SY -->
    fun mergeBitmaps(
        imageBitmap: Bitmap,
        imageBitmap2: Bitmap,
        isLTR: Boolean,
        centerMargin: Int,
        @ColorInt background: Int = Color.WHITE,
        progressCallback: ((Int) -> Unit)? = null,
    ): BufferedSource {
        val height = imageBitmap.height
        val width = imageBitmap.width
        val height2 = imageBitmap2.height
        val width2 = imageBitmap2.width

        val maxHeight = max(height, height2)

        val result = Bitmap.createBitmap(width + width2 + centerMargin, max(height, height2), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(background)
        val upperPart = Rect(
            if (isLTR) 0 else width2 + centerMargin,
            (maxHeight - height) / 2,
            (if (isLTR) 0 else width2 + centerMargin) + width,
            height + (maxHeight - height) / 2,
        )

        canvas.drawBitmap(imageBitmap, imageBitmap.rect, upperPart, null)
        progressCallback?.invoke(PROGRESS_FIRST_PAGE)
        val bottomPart = Rect(
            if (!isLTR) 0 else width + centerMargin,
            (maxHeight - height2) / 2,
            (if (!isLTR) 0 else width + centerMargin) + width2,
            height2 + (maxHeight - height2) / 2,
        )

        canvas.drawBitmap(imageBitmap2, imageBitmap2.rect, bottomPart, null)
        progressCallback?.invoke(PROGRESS_SECOND_PAGE)

        val output = Buffer()
        result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output.outputStream())
        progressCallback?.invoke(PROGRESS_DONE)
        return output
    }
}
