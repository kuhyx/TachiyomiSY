package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import coil3.BitmapImage
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.ViewSizeResolver
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.chrisbanes.photoview.PhotoView
import eu.kanade.tachiyomi.data.coil.cropBorders
import eu.kanade.tachiyomi.data.coil.customDecoder
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView.Config
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonSubsamplingImageView
import eu.kanade.tachiyomi.util.view.isVisibleOnScreen
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil
import uy.kohesive.injekt.api.get

internal fun ReaderPageImageView.prepareNonAnimatedImageView() {
    if (pageView is SubsamplingScaleImageView) return
    removeView(pageView)

    pageView = if (isWebtoon) {
        WebtoonSubsamplingImageView(context)
    } else {
        SubsamplingScaleImageView(context)
    }.apply {
        setMaxTileSize(ImageUtil.hardwareBitmapThreshold)
        setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
        setMinimumTileDpi(MIN_TILE_DPI)
        setOnStateChangedListener(
            object : SubsamplingScaleImageView.OnStateChangedListener {
                override fun onScaleChanged(newScale: Float, origin: Int) {
                    this@prepareNonAnimatedImageView.onScaleChanged(newScale)
                }

                override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                    // Not used
                }
            },
        )
        setOnClickListener { this@prepareNonAnimatedImageView.onViewClicked() }
    }
    addView(pageView, MATCH_PARENT, MATCH_PARENT)
}

internal fun ReaderPageImageView.setNonAnimatedImage(
    data: Any,
    config: Config,
) = (pageView as? SubsamplingScaleImageView)?.apply {
    setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
    setMinimumScaleType(config.minimumScaleType)
    setMinimumDpi(1) // Just so that very small image will be fit for initial load
    setCropBorders(config.cropBorders)
    setOnImageEventListener(
        object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
            override fun onReady() {
                setupZoom(config)
                if (isVisibleOnScreen()) landscapeZoom(true)
                this@setNonAnimatedImage.onImageLoaded()
            }

            override fun onImageLoadError(e: Exception) {
                this@setNonAnimatedImage.onImageLoadError(e)
            }
        },
    )

    when (data) {
        is BitmapDrawable -> {
            setImage(ImageSource.bitmap(data.bitmap))
            isVisible = true
        }
        is BufferedSource -> {
            if (!isWebtoon || alwaysDecodeLongStripWithSSIV) {
                setHardwareConfig(ImageUtil.canUseHardwareBitmap(data))
                setImage(ImageSource.inputStream(data.inputStream()))
                isVisible = true
                return@apply
            }

            ImageRequest.Builder(context)
                .data(data)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(
                    onSuccess = { result ->
                        val image = result as BitmapImage
                        setImage(ImageSource.bitmap(image.bitmap))
                        isVisible = true
                    },
                )
                .listener(
                    onError = { _, result ->
                        onImageLoadError(result.throwable)
                    },
                )
                .size(ViewSizeResolver(this@setNonAnimatedImage))
                .precision(Precision.INEXACT)
                .cropBorders(config.cropBorders)
                .customDecoder(true)
                .crossfade(false)
                .build()
                .let(context.imageLoader::enqueue)
        }
        else -> {
            throw IllegalArgumentException("Not implemented for class ${data::class.simpleName}")
        }
    }
}

internal fun ReaderPageImageView.prepareAnimatedImageView() {
    if (pageView is AppCompatImageView) return
    removeView(pageView)

    pageView = if (isWebtoon) {
        AppCompatImageView(context)
    } else {
        PhotoView(context)
    }.apply {
        adjustViewBounds = true

        if (this is PhotoView) {
            setScaleLevels(1F, 2F, MAX_ZOOM_SCALE)
            // Force 2 scale levels on double tap
            setOnDoubleTapListener(
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (scale > 1F) {
                            setScale(1F, e.x, e.y, true)
                        } else {
                            setScale(2F, e.x, e.y, true)
                        }
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        this@prepareAnimatedImageView.onViewClicked()
                        return super.onSingleTapConfirmed(e)
                    }
                },
            )
            setOnScaleChangeListener { _, _, _ ->
                this@prepareAnimatedImageView.onScaleChanged(scale)
            }
        }
    }
    addView(pageView, MATCH_PARENT, MATCH_PARENT)
}

internal fun ReaderPageImageView.setAnimatedImage(
    data: Any,
    config: Config,
) = (pageView as? AppCompatImageView)?.apply {
    if (this is PhotoView) {
        setZoomTransitionDuration(config.zoomDuration.getSystemScaledDuration())
    }

    val request = ImageRequest.Builder(context)
        .data(data)
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .target(
            onSuccess = { result ->
                val drawable = result.asDrawable(context.resources)
                setImageDrawable(drawable)
                (drawable as? Animatable)?.start()
                isVisible = true
                this@setAnimatedImage.onImageLoaded()
            },
        )
        .listener(
            onError = { _, result ->
                onImageLoadError(result.throwable)
            },
        )
        .crossfade(false)
        .build()
    context.imageLoader.enqueue(request)
}
