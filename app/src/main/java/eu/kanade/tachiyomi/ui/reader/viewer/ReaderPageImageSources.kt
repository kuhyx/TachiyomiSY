package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import coil3.dispose
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.Config
import okio.BufferedSource
import uy.kohesive.injekt.api.get

internal fun ReaderPageImageView.setImage(drawable: Drawable, config: Config) {
    this.config = config
    if (drawable is Animatable) {
        prepareAnimatedImageView()
        setAnimatedImage(drawable, config)
    } else {
        prepareNonAnimatedImageView()
        setNonAnimatedImage(drawable, config)
    }
}

internal fun ReaderPageImageView.setImage(source: BufferedSource, isAnimated: Boolean, config: Config) {
    this.config = config
    if (isAnimated) {
        prepareAnimatedImageView()
        setAnimatedImage(source, config)
    } else {
        prepareNonAnimatedImageView()
        setNonAnimatedImage(source, config)
    }
}

// The page view is always one of the two the prepare functions create.
internal fun ReaderPageImageView.recycle() = pageView?.let {
    if (it is SubsamplingScaleImageView) it.recycle() else (it as AppCompatImageView).dispose()
    it.isVisible = false
}
