package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.viewer.GestureDetectorWithLongTap
import kotlin.math.abs
import kotlin.math.sign

/**
 * Implementation of a [RecyclerView] used by the webtoon reader.
 */
internal class WebtoonRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    internal var isZooming = false
    internal var atLastPosition = false
    internal var atFirstPosition = false
    internal var halfWidth = 0
    internal var halfHeight = 0
    var originalHeight = 0
        private set
    private var heightSet = false
    private var firstVisibleItemPosition = 0
    private var lastVisibleItemPosition = 0
    internal var currentScale = DEFAULT_RATE
    var zoomOutDisabled = false
        set(value) {
            field = value
            if (value && currentScale < DEFAULT_RATE) {
                zoom(currentScale, DEFAULT_RATE, x, 0f, y, 0f)
            }
        }
    internal val minRate
        get() = if (zoomOutDisabled) DEFAULT_RATE else MIN_RATE

    private val listener = GestureListener()
    internal val detector = Detector()

    var doubleTapZoom = true

    var tapListener: ((MotionEvent) -> Unit)? = null
    var longTapListener: ((MotionEvent) -> Boolean)? = null

    private var isManuallyScrolling = false
    private var tapDuringManualScroll = false

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        halfWidth = MeasureSpec.getSize(widthSpec) / 2
        halfHeight = MeasureSpec.getSize(heightSpec) / 2
        if (!heightSet) {
            originalHeight = MeasureSpec.getSize(heightSpec)
            heightSet = true
        }
        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> tapDuringManualScroll = isManuallyScrolling
            MotionEvent.ACTION_UP -> performClick()
        }

        detector.onTouchEvent(e)
        return super.onTouchEvent(e)
    }

    // Taps are detected by the gesture detector; this only keeps accessibility services' click
    // action routed through the standard path.
    override fun performClick(): Boolean = super.performClick()

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        val layoutManager = layoutManager
        lastVisibleItemPosition =
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        val layoutManager = layoutManager
        val visibleItemCount = layoutManager?.childCount ?: 0
        val totalItemCount = layoutManager?.itemCount ?: 0
        atLastPosition = visibleItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1
        atFirstPosition = firstVisibleItemPosition == 0

        if (state == SCROLL_STATE_IDLE) {
            isManuallyScrolling = false
        }
    }

    fun onManualScroll() {
        isManuallyScrolling = true
    }

    inner class GestureListener : GestureDetectorWithLongTap.Listener() {

        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            if (!tapDuringManualScroll) {
                tapListener?.invoke(ev)
            }
            return false
        }

        override fun onDoubleTap(ev: MotionEvent): Boolean {
            detector.isDoubleTapping = true
            return false
        }

        fun onDoubleTapConfirmed(ev: MotionEvent) {
            if (!isZooming && doubleTapZoom) {
                if (scaleX != DEFAULT_RATE) {
                    zoom(currentScale, DEFAULT_RATE, x, 0f, y, 0f)
                    layoutParams.height = originalHeight
                    halfHeight = layoutParams.height / 2
                    requestLayout()
                } else {
                    val toScale = 2f
                    val toX = (halfWidth - ev.x) * (toScale - 1)
                    val toY = (halfHeight - ev.y) * (toScale - 1)
                    zoom(DEFAULT_RATE, toScale, 0f, toX, 0f, toY)
                }
            }
        }

        override fun onLongTapConfirmed(ev: MotionEvent) {
            if (longTapListener?.invoke(ev) == true) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    inner class Detector : GestureDetectorWithLongTap(context, listener) {

        private var scrollPointerId = 0
        private var downX = 0
        private var downY = 0
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var isZoomDragging = false
        var isDoubleTapping = false
        var isQuickScaling = false

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            val actionIndex = ev.actionIndex
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    trackPointer(ev, 0)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    trackPointer(ev, actionIndex)
                }
                MotionEvent.ACTION_MOVE -> {
                    onMove(ev)?.let { return it }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDoubleTapping && !isQuickScaling) {
                        listener.onDoubleTapConfirmed(ev)
                    }
                    resetGesture()
                }
                MotionEvent.ACTION_CANCEL -> {
                    resetGesture()
                }
            }
            return super.onTouchEvent(ev)
        }

        private fun trackPointer(ev: MotionEvent, pointerIndex: Int) {
            scrollPointerId = ev.getPointerId(pointerIndex)
            downX = ev.getX(pointerIndex).roundToPixel()
            downY = ev.getY(pointerIndex).roundToPixel()
        }

        private fun resetGesture() {
            isZoomDragging = false
            isDoubleTapping = false
            isQuickScaling = false
        }

        // Drags the zoomed content; a non-null result is the event's final answer, null falls through to super.
        private fun onMove(ev: MotionEvent): Boolean? {
            if (isDoubleTapping && isQuickScaling) return true
            val index = ev.findPointerIndex(scrollPointerId)
            if (index < 0) return false

            val x = ev.getX(index).roundToPixel()
            val y = ev.getY(index).roundToPixel()
            var dx = x - downX
            var dy = if (atFirstPosition || atLastPosition) y - downY else 0
            if (!isZoomDragging && currentScale > 1f) {
                val (eatenX, eatenY) = startDragPastSlop(dx, dy)
                dx = eatenX
                dy = eatenY
            }
            if (isZoomDragging) {
                zoomScrollBy(dx, dy)
            }
            return null
        }

        // Start dragging once either axis moves past the touch slop, and eat the slop on that axis.
        private fun startDragPastSlop(dx: Int, dy: Int): Pair<Int, Int> {
            val pastSlopX = abs(dx) > touchSlop
            val pastSlopY = abs(dy) > touchSlop
            if (pastSlopX || pastSlopY) isZoomDragging = true
            return Pair(
                if (pastSlopX) dx - touchSlop * dx.sign else dx,
                if (pastSlopY) dy - touchSlop * dy.sign else dy,
            )
        }
    }
}

// Rounds a touch coordinate to the nearest pixel.
internal fun Float.roundToPixel(): Int = (this + HALF).toInt()
private const val HALF = 0.5f
private const val MIN_RATE = 0.5f
internal const val DEFAULT_RATE = 1f
