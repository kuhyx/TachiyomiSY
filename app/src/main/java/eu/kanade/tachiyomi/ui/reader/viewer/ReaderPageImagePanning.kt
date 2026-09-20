package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_OUT_QUAD
import uy.kohesive.injekt.api.get

/**
 * Check if the image can be panned to the left.
 */
internal fun ReaderPageImageView.canPanLeft(): Boolean = canPan { it.left }

/**
 * Check if the image can be panned to the right.
 */
internal fun ReaderPageImageView.canPanRight(): Boolean = canPan { it.right }

// Check whether the image can be panned.
// @param fn a function that returns the direction to check for
internal fun ReaderPageImageView.canPan(fn: (RectF) -> Float): Boolean {
    (pageView as? SubsamplingScaleImageView)?.let { view ->
        RectF().let {
            view.getPanRemaining(it)
            return fn(it) > 1
        }
    }
    return false
}

/**
 * Pans the image to the left by a screen's width worth.
 */
internal fun ReaderPageImageView.panLeft() {
    pan { center, view -> center.also { it.x -= view.width / view.scale } }
}

/**
 * Pans the image to the right by a screen's width worth.
 */
internal fun ReaderPageImageView.panRight() {
    pan { center, view -> center.also { it.x += view.width / view.scale } }
}

// Pans the image.
// @param fn a function that computes the new center of the image
internal fun ReaderPageImageView.pan(fn: (PointF, SubsamplingScaleImageView) -> PointF) {
    (pageView as? SubsamplingScaleImageView)?.let { view ->

        val target = fn(view.center ?: return, view)
        view.animateCenter(target)!!
            .withEasing(EASE_OUT_QUAD)
            .withDuration(DOUBLE_TAP_ANIMATION_MS)
            .withInterruptible(true)
            .start()
    }
}
