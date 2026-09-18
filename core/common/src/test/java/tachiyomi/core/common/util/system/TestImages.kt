package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.hippo.unifile.UniFile
import okio.Buffer
import okio.BufferedSource

/** Encoded test images, built with the real Skia-backed Bitmap under Robolectric's native graphics mode. */
internal object TestImages {
    private const val JPEG_QUALITY = 100
    private const val JPEG_TOLERANCE = 8

    /** An ARGB bitmap filled with [color]. */
    fun bitmap(width: Int, height: Int, color: Int = Color.WHITE): Bitmap =
        createBitmap(width, height).apply { eraseColor(color) }

    /** [bitmap] encoded as PNG (lossless, so pixel assertions survive the round trip). */
    fun png(bitmap: Bitmap): BufferedSource = encode(bitmap, Bitmap.CompressFormat.PNG)

    /** [bitmap] encoded as JPEG. */
    fun jpeg(bitmap: Bitmap): BufferedSource = encode(bitmap, Bitmap.CompressFormat.JPEG)

    /** A PNG whose left half is [left] and right half is [right]. */
    fun halves(width: Int, height: Int, left: Int, right: Int): BufferedSource {
        val bitmap = bitmap(width, height, left)
        for (y in 0..<height) for (x in width / 2..<width) bitmap.setPixel(x, y, right)
        return png(bitmap)
    }

    /** A PNG whose rows above [split] are [top] and the rest [bottom]. */
    fun bands(width: Int, height: Int, split: Int, top: Int, bottom: Int): BufferedSource {
        val bitmap = bitmap(width, height, bottom)
        for (y in 0..<split) for (x in 0..<width) bitmap.setPixel(x, y, top)
        return png(bitmap)
    }

    /** True when the pixel at ([x], [y]) is [color] up to JPEG rounding. */
    fun Bitmap.pixelIsNear(x: Int, y: Int, color: Int): Boolean {
        val pixel = getPixel(x, y)
        return listOf(Color::red, Color::green, Color::blue).all { channel ->
            kotlin.math.abs(channel(pixel) - channel(color)) <= JPEG_TOLERANCE
        }
    }

    /** [file] as a [UniFile]; the annotated factory is nullable but never null for a plain file. */
    fun uniFile(file: java.io.File): UniFile = requireNotNull(UniFile.fromFile(file)) { "no UniFile for $file" }

    /** Decodes [source] again so a test can look at what an operation produced. */
    fun decode(source: BufferedSource): Bitmap =
        requireNotNull(android.graphics.BitmapFactory.decodeStream(source.peek().inputStream())) { "undecodable" }

    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat): BufferedSource {
        val buffer = Buffer()
        bitmap.compress(format, JPEG_QUALITY, buffer.outputStream())
        return buffer
    }
}
