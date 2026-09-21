package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

private const val ANIMATOR_DURATION_TIME = 200
private const val FLING_ANIMATION_MS = 400L
private const val FLING_DISTANCE_TIME_FACTOR = 0.4f
private const val MAX_SCALE_RATE = 3f

internal fun WebtoonRecyclerView.getPositionX(positionX: Float): Float {
    if (currentScale < 1) {
        return 0f
    }
    val maxPositionX = halfWidth * (currentScale - 1)
    return positionX.coerceIn(-maxPositionX, maxPositionX)
}

internal fun WebtoonRecyclerView.getPositionY(positionY: Float): Float {
    if (currentScale < 1) {
        return (originalHeight / 2 - halfHeight).toFloat()
    }
    val maxPositionY = halfHeight * (currentScale - 1)
    return positionY.coerceIn(-maxPositionY, maxPositionY)
}

internal fun WebtoonRecyclerView.zoom(
    fromRate: Float,
    toRate: Float,
    fromX: Float,
    toX: Float,
    fromY: Float,
    toY: Float,
) {
    isZooming = true
    val animatorSet = AnimatorSet()
    val translationXAnimator = ValueAnimator.ofFloat(fromX, toX)
    translationXAnimator.addUpdateListener { animation -> x = animation.animatedValue as Float }

    val translationYAnimator = ValueAnimator.ofFloat(fromY, toY)
    translationYAnimator.addUpdateListener { animation -> y = animation.animatedValue as Float }

    val scaleAnimator = ValueAnimator.ofFloat(fromRate, toRate)
    scaleAnimator.addUpdateListener { animation ->
        currentScale = animation.animatedValue as Float
        setScaleRate(currentScale)
    }
    animatorSet.playTogether(translationXAnimator, translationYAnimator, scaleAnimator)
    animatorSet.duration = ANIMATOR_DURATION_TIME.toLong()
    animatorSet.interpolator = DecelerateInterpolator()
    animatorSet.start()
    animatorSet.doOnEnd {
        isZooming = false
        currentScale = toRate
    }
}

internal fun WebtoonRecyclerView.zoomFling(velocityX: Int, velocityY: Int): Boolean {
    if (currentScale <= 1f) return false

    val distanceTimeFactor = FLING_DISTANCE_TIME_FACTOR
    val animatorSet = AnimatorSet()

    if (velocityX != 0) {
        val dx = distanceTimeFactor * velocityX / 2
        val newX = getPositionX(x + dx)
        val translationXAnimator = ValueAnimator.ofFloat(x, newX)
        translationXAnimator.addUpdateListener { animation -> x = getPositionX(animation.animatedValue as Float) }
        animatorSet.play(translationXAnimator)
    }
    if (velocityY != 0 && (atFirstPosition || atLastPosition)) {
        val dy = distanceTimeFactor * velocityY / 2
        val newY = getPositionY(y + dy)
        val translationYAnimator = ValueAnimator.ofFloat(y, newY)
        translationYAnimator.addUpdateListener { animation -> y = getPositionY(animation.animatedValue as Float) }
        animatorSet.play(translationYAnimator)
    }

    animatorSet.duration = FLING_ANIMATION_MS
    animatorSet.interpolator = DecelerateInterpolator()
    animatorSet.start()

    return true
}

internal fun WebtoonRecyclerView.zoomScrollBy(dx: Int, dy: Int) {
    if (dx != 0) {
        x = getPositionX(x + dx)
    }
    if (dy != 0) {
        y = getPositionY(y + dy)
    }
}

internal fun WebtoonRecyclerView.setScaleRate(rate: Float) {
    scaleX = rate
    scaleY = rate
}

internal fun WebtoonRecyclerView.onScale(scaleFactor: Float) {
    currentScale *= scaleFactor
    currentScale = currentScale.coerceIn(
        minRate,
        MAX_SCALE_RATE,
    )

    setScaleRate(currentScale)

    layoutParams.height = if (currentScale < 1) {
        (originalHeight / currentScale).toInt()
    } else {
        originalHeight
    }
    halfHeight = layoutParams.height / 2

    if (currentScale != DEFAULT_RATE) {
        x = getPositionX(x)
        y = getPositionY(y)
    } else {
        x = 0f
        y = 0f
    }

    requestLayout()
}

internal fun WebtoonRecyclerView.onScaleBegin() {
    if (detector.isDoubleTapping) {
        detector.isQuickScaling = true
    }
}

internal fun WebtoonRecyclerView.onScaleEnd() {
    if (scaleX < minRate) {
        zoom(currentScale, minRate, x, 0f, y, 0f)
    }
}
