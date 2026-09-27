package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The webtoon viewer's small parts: the base holder, the touch-blind image view and the frame's zoom flag. */
@RunWith(RobolectricTestRunner::class)
internal class WebtoonPartsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun baseHolderRecyclesNothing() {
        val holder = object : WebtoonBaseHolder(View(context), mockk(relaxed = true)) {}
        holder.recycle()
        holder.context shouldBe context
    }

    @Test
    fun imageViewIgnoresTouches() {
        val view = WebtoonSubsamplingImageView(context)
        view.dispatchTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0)) shouldBe false
    }

    @Test
    fun frameRemembersZoomOut() {
        val frame = WebtoonFrame(context)
        frame.zoomOutDisabled shouldBe false
        frame.zoomOutDisabled = true
        frame.zoomOutDisabled shouldBe true
    }
}
