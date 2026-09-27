package androidx.recyclerview.widget

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PARENT_PX = 100

/** Fixed-size blank rows, [sizePx] along the scroll axis. */
private class Rows(private val count: Int, private val sizePx: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = View(parent.context)
        view.layoutParams = RecyclerView.LayoutParams(sizePx, sizePx)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = count
}

@RunWith(RobolectricTestRunner::class)
internal class WebtoonLayoutManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun laidOut(count: Int, sizePx: Int, horizontal: Boolean = false, scrollPx: Int = 0): WebtoonLayoutManager {
        val manager = WebtoonLayoutManager(context, extraLayoutSpace = 10)
        if (horizontal) manager.orientation = RecyclerView.HORIZONTAL
        val list = RecyclerView(context)
        list.layoutManager = manager
        list.adapter = Rows(count, sizePx)
        val spec = MeasureSpec.makeMeasureSpec(PARENT_PX, MeasureSpec.EXACTLY)
        list.measure(spec, spec)
        list.layout(0, 0, PARENT_PX, PARENT_PX)
        if (scrollPx != 0) {
            if (horizontal) list.scrollBy(scrollPx, 0) else list.scrollBy(0, scrollPx)
            list.measure(spec, spec)
            list.layout(0, 0, PARENT_PX, PARENT_PX)
        }
        return manager
    }

    @Test
    fun lastFullyShownRowIsFound() {
        val manager = laidOut(count = 5, sizePx = 40)
        manager.findLastEndVisibleItemPosition() shouldBe 1
        manager.isItemPrefetchEnabled shouldBe false
    }

    @Test
    fun horizontalUsesItsBounds() {
        laidOut(count = 5, sizePx = 40, horizontal = true).findLastEndVisibleItemPosition() shouldBe 1
    }

    @Test
    fun oversizedRowCounts() {
        laidOut(count = 2, sizePx = 300, scrollPx = 20).findLastEndVisibleItemPosition() shouldBe 0
    }

    @Test
    fun anEmptyListHasNoPosition() {
        laidOut(count = 0, sizePx = 40).findLastEndVisibleItemPosition() shouldBe RecyclerView.NO_POSITION
    }
}
