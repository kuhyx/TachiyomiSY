package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Xml
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class ReaderNavigationOverlayViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val overlay = ReaderNavigationOverlayView(
        context,
        Xml.asAttributeSet(context.resources.getLayout(R.layout.reader_activity)),
    ).apply {
        visibility = View.GONE
        layout(0, 0, 100, 200)
    }

    private fun fade() = ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)

    private fun draw() = overlay.draw(Canvas(Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)))

    @Test
    fun firstLaunchMayStayHidden() {
        draw()
        overlay.setNavigation(LNavigation(), showOnStart = false)
        fade()
        overlay.isVisible shouldBe false
        overlay.setNavigation(DisabledNavigation(), showOnStart = true)
        fade()
        overlay.isVisible shouldBe false
        draw()
    }

    @Test
    fun showsThenHidesOnTouch() {
        overlay.setNavigation(LNavigation(), showOnStart = true)
        fade()
        overlay.isVisible shouldBe true
        overlay.setNavigation(LNavigation(), showOnStart = true)
        draw()
        overlay.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        overlay.performClick()
        fade()
        overlay.isVisible shouldBe false
        overlay.performClick() shouldBe true
    }
}
