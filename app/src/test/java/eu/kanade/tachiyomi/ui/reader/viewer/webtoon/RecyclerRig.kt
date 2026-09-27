package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.robolectric.Robolectric

/** A motion event with one pointer per entry of [points], pointer ids counting up from 0. */
internal fun touch(
    action: Int,
    vararg points: Pair<Float, Float>,
    downTime: Long = 0L,
    eventTime: Long = 0L,
): MotionEvent {
    val properties = Array(points.size) { index ->
        MotionEvent.PointerProperties().apply {
            id = index
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    }
    val coords = Array(points.size) { index ->
        MotionEvent.PointerCoords().apply {
            x = points[index].first
            y = points[index].second
        }
    }
    return MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        points.size,
        properties,
        coords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0,
    )
}

/** A webtoon list of [count] plain rows, measured and laid out at 400 x 800. */
internal fun recycler(count: Int = 5): WebtoonRecyclerView {
    val context = Robolectric.buildActivity(Activity::class.java).setup().get()
    return WebtoonRecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        adapter = Rows(count)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )
        context.setContentView(this, ViewGroup.LayoutParams(WIDTH, HEIGHT))
        measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, WIDTH, HEIGHT)
    }
}

private class Rows(private val count: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(View(parent.context).apply { minimumHeight = ROW }) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = count
}

internal const val WIDTH = 400
internal const val HEIGHT = 800
private const val ROW = 300
