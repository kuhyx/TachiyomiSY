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

/** Joins two pages side by side into one image. */
internal object BitmapMerging {
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
        progressCallback?.invoke(98)
        val bottomPart = Rect(
            if (!isLTR) 0 else width + centerMargin,
            (maxHeight - height2) / 2,
            (if (!isLTR) 0 else width + centerMargin) + width2,
            height2 + (maxHeight - height2) / 2,
        )

        canvas.drawBitmap(imageBitmap2, imageBitmap2.rect, bottomPart, null)
        progressCallback?.invoke(99)

        val output = Buffer()
        result.compress(Bitmap.CompressFormat.JPEG, 100, output.outputStream())
        progressCallback?.invoke(100)
        return output
    }

    val Bitmap.rect: Rect
        get() = Rect(0, 0, width, height)
    // SY <--
}
