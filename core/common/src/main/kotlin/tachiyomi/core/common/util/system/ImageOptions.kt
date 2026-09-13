package tachiyomi.core.common.util.system

import android.graphics.BitmapFactory
import okio.BufferedSource

/**
 * Used to check an image's dimensions without loading it in the memory.
 */
internal fun extractImageOptions(imageSource: BufferedSource): BitmapFactory.Options {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeStream(imageSource.peek().inputStream(), null, options)
    return options
}
