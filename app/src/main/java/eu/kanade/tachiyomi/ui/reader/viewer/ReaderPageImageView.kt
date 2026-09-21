package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.core.os.postDelayed
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_IN_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// A wrapper view for showing page image.
// Animated image will be drawn by [PhotoView] while [SubsamplingScaleImageView] will take non-animated image.
// @param isWebtoon if true, [WebtoonSubsamplingImageView] will be used instead of [SubsamplingScaleImageView]
// and [AppCompatImageView] will be used instead of [PhotoView]
private const val ZOOM_ANIMATION_MS = 500L
internal const val DOUBLE_TAP_ANIMATION_MS = 250L
internal const val MIN_TILE_DPI = 180

internal open class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttrs: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    internal val isWebtoon: Boolean = false,
) : FrameLayout(context, attrs, defStyleAttrs, defStyleRes) {

    internal val alwaysDecodeLongStripWithSSIV by lazy {
        Injekt.get<BasePreferences>().alwaysDecodeLongStripWithSSIV.get()
    }

    internal var pageView: View? = null

    internal var config: Config? = null

    var onImageLoaded: (() -> Unit)? = null
    var onImageLoadError: ((Throwable?) -> Unit)? = null
    var onScaleChanged: ((newScale: Float) -> Unit)? = null
    var onViewClicked: (() -> Unit)? = null

    /**
     * For automatic background. Will be set as background color when [onImageLoaded] is called.
     */
    var pageBackground: Drawable? = null

    @CallSuper
    open fun onImageLoaded() {
        onImageLoaded?.invoke()
        background = pageBackground
    }

    @CallSuper
    open fun onImageLoadError(error: Throwable?) {
        onImageLoadError?.invoke(error)
    }

    @CallSuper
    open fun onScaleChanged(newScale: Float) {
        onScaleChanged?.invoke(newScale)
    }

    @CallSuper
    open fun onViewClicked() {
        onViewClicked?.invoke()
    }

    open fun onPageSelected(forward: Boolean) {
        with(pageView as? SubsamplingScaleImageView) {
            if (this == null) return
            if (isReady) {
                landscapeZoom(forward)
            } else {
                setOnImageEventListener(
                    object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        override fun onReady() {
                            setupZoom(config)
                            landscapeZoom(forward)
                            this@ReaderPageImageView.onImageLoaded()
                        }

                        override fun onImageLoadError(e: Exception) {
                            onImageLoadError(e)
                        }
                    },
                )
            }
        }
    }

    internal fun SubsamplingScaleImageView.landscapeZoom(forward: Boolean) {
        val config = config
        val zoomsLandscape =
            config != null && config.landscapeZoom && config.minimumScaleType == SCALE_TYPE_CENTER_INSIDE
        if (zoomsLandscape && sWidth > sHeight && scale == minScale) {
            handler?.postDelayed(ZOOM_ANIMATION_MS) {
                val targetScale = height.toFloat() / sHeight.toFloat()
                animateScaleAndCenter(targetScale, zoomStartPoint(config.zoomStartPosition, forward))
                    ?.withDuration(ZOOM_ANIMATION_MS)
                    ?.withEasing(EASE_IN_OUT_QUAD)
                    ?.withInterruptible(true)
                    ?.start()
            }
        }
    }

    // The edge the zoom starts from; reading backwards starts from the opposite edge.
    private fun SubsamplingScaleImageView.zoomStartPoint(position: ZoomStartPosition, forward: Boolean): PointF? {
        val leftEdge = PointF(0F, 0F)
        val rightEdge = PointF(sWidth.toFloat(), 0F)
        return when (position) {
            ZoomStartPosition.LEFT -> if (forward) leftEdge else rightEdge
            ZoomStartPosition.RIGHT -> if (forward) rightEdge else leftEdge
            ZoomStartPosition.CENTER -> center
        }
    }

    internal fun SubsamplingScaleImageView.setupZoom(config: Config?) {
        // 5x zoom
        maxScale = scale * MAX_ZOOM_SCALE
        setDoubleTapZoomScale(scale * 2)

        when (config?.zoomStartPosition) {
            ZoomStartPosition.LEFT -> setScaleAndCenter(scale, PointF(0F, 0F))
            ZoomStartPosition.RIGHT -> setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0F))
            ZoomStartPosition.CENTER -> setScaleAndCenter(scale, center)
            null -> {}
        }
    }

    internal fun Int.getSystemScaledDuration(): Int = (this * context.animatorDurationScale).toInt().coerceAtLeast(1)

    /**
     * All of the config except [zoomDuration] will only be used for non-animated image.
     */
    data class Config(
        val zoomDuration: Int,
        val minimumScaleType: Int = SCALE_TYPE_CENTER_INSIDE,
        val cropBorders: Boolean = false,
        val zoomStartPosition: ZoomStartPosition = ZoomStartPosition.CENTER,
        val landscapeZoom: Boolean = false,
    )

    enum class ZoomStartPosition {
        LEFT,
        CENTER,
        RIGHT,
    }
}

internal const val MAX_ZOOM_SCALE = 5F
