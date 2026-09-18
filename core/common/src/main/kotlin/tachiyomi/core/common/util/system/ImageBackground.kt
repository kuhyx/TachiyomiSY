package tachiyomi.core.common.util.system

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.drawable.toDrawable
import okio.BufferedSource
import tachiyomi.decoder.ImageDecoder

/** Picks the page background that best continues a page's edges. */
internal object ImageBackground {
    /**
     * Decodes [imageSource] and asks [BackgroundChooser] what to put behind it; an undecodable
     * image gets white.
     */
    fun chooseBackground(context: Context, imageSource: BufferedSource): Drawable {
        val decoder = ImageDecoder.newInstance(imageSource.inputStream())
        val image = decoder?.decode()
        decoder?.recycle()
        if (image == null) return Color.WHITE.toDrawable()

        val isLandscape = context.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
        return BackgroundChooser.choose(BitmapPixelGrid(image), isLandscape).toDrawable()
    }

    private fun PageBackground.toDrawable(): Drawable = when (this) {
        is PageBackground.Solid -> color.toDrawable()
        is PageBackground.Gradient -> GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors.toIntArray())
    }
}
