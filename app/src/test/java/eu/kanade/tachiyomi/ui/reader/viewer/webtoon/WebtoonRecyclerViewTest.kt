package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** A motion event at ([x], [y]) with [action]. */
internal fun motion(action: Int, x: Float = 10f, y: Float = 10f): MotionEvent =
    MotionEvent.obtain(0L, 0L, action, x, y, 0)

/** Runs every pending animation frame to its end. */
internal fun finishAnimations() = ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)

@RunWith(RobolectricTestRunner::class)
internal class WebtoonRecyclerViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val recycler = WebtoonRecyclerView(context).apply {
        layoutParams = ViewGroup.LayoutParams(400, 800)
        layoutManager = LinearLayoutManager(context)
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = 20

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(View(parent.context).apply { minimumHeight = 100 }) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
        }
        measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, 400, 800)
    }

    @Test
    fun measureRemembersFirstHeight() {
        recycler.originalHeight shouldBe 800
        recycler.halfWidth shouldBe 200
        recycler.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        recycler.originalHeight shouldBe 800
        recycler.halfHeight shouldBe 300
    }

    @Test
    fun positionsClampByScale() {
        recycler.currentScale = 0.5f
        recycler.getPositionX(50f) shouldBe 0f
        recycler.getPositionY(50f) shouldBe 0f
        recycler.currentScale = 2f
        recycler.getPositionX(500f) shouldBe 200f
        recycler.getPositionY(-900f) shouldBe -400f
        recycler.zoomScrollBy(0, 0)
        recycler.zoomScrollBy(30, 40)
        recycler.x shouldBe 30f
        recycler.y shouldBe 40f
    }

    @Test
    fun scalingAdjustsLayout() {
        recycler.onScale(0.5f)
        recycler.layoutParams.height shouldBe 1600
        recycler.onScale(2f)
        recycler.x shouldBe 0f
        recycler.onScale(2f)
        recycler.layoutParams.height shouldBe 800
        recycler.onScaleEnd()
        recycler.zoomOutDisabled = true
        recycler.onScale(0.1f)
        recycler.currentScale shouldBe DEFAULT_RATE
        recycler.zoomOutDisabled = false
        recycler.onScale(0.5f)
        recycler.onScaleEnd()
        finishAnimations()
    }

    @Test
    fun zoomAnimatesToTarget() {
        recycler.zoom(1f, 2f, 0f, 10f, 0f, 20f)
        recycler.isZooming shouldBe true
        finishAnimations()
        recycler.isZooming shouldBe false
        recycler.currentScale shouldBe 2f
        recycler.currentScale = 0.5f
        recycler.zoomOutDisabled = true
        finishAnimations()
        recycler.currentScale shouldBe 1f
        recycler.zoomOutDisabled = true
    }

    @Test
    fun flingOnlyWhenZoomed() {
        recycler.zoomFling(100, 100) shouldBe false
        recycler.currentScale = 2f
        recycler.zoomFling(0, 0) shouldBe true
        recycler.zoomFling(1000, 1000) shouldBe true
        recycler.atFirstPosition = true
        recycler.zoomFling(1000, 1000) shouldBe true
        recycler.atFirstPosition = false
        recycler.atLastPosition = true
        recycler.zoomFling(1000, -1000) shouldBe true
        finishAnimations()
    }

    @Test
    fun scrollStateTracksEnds() {
        recycler.scrollBy(0, 100)
        recycler.onScrollStateChanged(RecyclerView.SCROLL_STATE_DRAGGING)
        recycler.atFirstPosition shouldBe false
        recycler.scrollBy(0, 5000)
        recycler.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
        recycler.atLastPosition shouldBe true
        val bare = WebtoonRecyclerView(context)
        bare.onScrollStateChanged(RecyclerView.SCROLL_STATE_IDLE)
        bare.atLastPosition shouldBe false
    }
}
