package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.get
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.abs

/** Read-only access to an image's pixels, so the background heuristics run without Android. */
internal interface PixelGrid {
    val width: Int
    val height: Int

    /** The ARGB colour at ([x], [y]). */
    operator fun get(x: Int, y: Int): Int
}

/** A [PixelGrid] over a decoded [Bitmap]. */
internal class BitmapPixelGrid(private val bitmap: Bitmap) : PixelGrid {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun get(x: Int, y: Int): Int = bitmap[x, y]
}

/** What a page should sit on: one colour, or a vertical gradient listed from top to bottom. */
internal sealed interface PageBackground {
    data class Solid(@ColorInt val color: Int) : PageBackground
    data class Gradient(val colors: List<Int>) : PageBackground
}

/** Near-black and opaque. */
internal fun @receiver:ColorInt Int.isDark(): Boolean =
    red < DARK_CHANNEL_MAX && blue < DARK_CHANNEL_MAX && green < DARK_CHANNEL_MAX && alpha > DARK_ALPHA_MIN

/** Every channel within [CLOSE_CHANNEL_DELTA] of [other]'s. */
internal fun @receiver:ColorInt Int.isCloseTo(other: Int): Boolean =
    abs(red - other.red) < CLOSE_CHANNEL_DELTA &&
        abs(green - other.green) < CLOSE_CHANNEL_DELTA &&
        abs(blue - other.blue) < CLOSE_CHANNEL_DELTA

/** Near-white: the three channels sum above [WHITE_CHANNEL_SUM_MIN]. */
internal fun @receiver:ColorInt Int.isWhite(): Boolean =
    red + blue + green > WHITE_CHANNEL_SUM_MIN

private const val DARK_CHANNEL_MAX = 40
private const val DARK_ALPHA_MIN = 200
private const val CLOSE_CHANNEL_DELTA = 30
private const val WHITE_CHANNEL_SUM_MIN = 740
