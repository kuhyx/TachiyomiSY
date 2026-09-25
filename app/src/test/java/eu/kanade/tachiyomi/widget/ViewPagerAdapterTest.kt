package eu.kanade.tachiyomi.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ViewPagerAdapterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val container = FrameLayout(context)

    private open class Pages(private val context: Context) : ViewPagerAdapter() {
        override fun getCount(): Int = 2

        override fun createView(container: ViewGroup, position: Int): View = View(context).apply { tag = position }
    }

    private class TrackingPages(context: Context) : Pages(context) {
        val destroyed = mutableListOf<Int>()

        override fun destroyView(container: ViewGroup, position: Int, view: View) {
            destroyed += position
        }
    }

    @Test
    fun instantiateAddsTheView() {
        val adapter = Pages(context)
        val page = adapter.instantiateItem(container, 1) as View
        container.childCount shouldBe 1
        page.tag shouldBe 1
        adapter.isViewFromObject(page, page) shouldBe true
        adapter.isViewFromObject(page, Any()) shouldBe false
    }

    @Test
    fun destroyRemovesTheView() {
        val adapter = Pages(context)
        val page = adapter.instantiateItem(container, 0)
        adapter.destroyItem(container, 0, page)
        container.childCount shouldBe 0
    }

    @Test
    fun destroyCallsTheHook() {
        val adapter = TrackingPages(context)
        val page = adapter.instantiateItem(container, 1)
        adapter.destroyItem(container, 1, page)
        adapter.destroyed shouldBe listOf(1)
        container.childCount shouldBe 0
    }
}
