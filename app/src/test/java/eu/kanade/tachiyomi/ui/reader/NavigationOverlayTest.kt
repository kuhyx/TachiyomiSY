package eu.kanade.tachiyomi.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Xml
import android.view.MotionEvent
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

/** The overlay that shows the tap zones: fading in on a new layout, out on the first touch, and drawing. */
@RunWith(RobolectricTestRunner::class)
internal class NavigationOverlayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Any layout's attributes will do: the overlay reads none of its own.
    private val overlay = ReaderNavigationOverlayView(
        context,
        Xml.asAttributeSet(context.resources.getLayout(R.layout.reader_error)),
    ).apply {
        isVisible = false
        layout(0, 0, 100, 200)
    }

    private fun settle() = ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)

    @Test
    fun firstLayoutShowsOnlyWhenAsked() {
        overlay.setNavigation(LNavigation(), showOnStart = false)
        settle()
        overlay.isVisible shouldBe false
        overlay.setNavigation(LNavigation(), showOnStart = false)
        settle()
        overlay.isVisible shouldBe true
        overlay.setNavigation(LNavigation(), showOnStart = true)
        overlay.isVisible shouldBe true
    }

    @Test
    fun disabledNavigationStaysHidden() {
        overlay.setNavigation(DisabledNavigation(), showOnStart = true)
        settle()
        overlay.isVisible shouldBe false
    }

    @Test
    fun touchFadesOut() {
        overlay.performClick()
        overlay.setNavigation(LNavigation(), showOnStart = true)
        settle()
        overlay.onTouchEvent(MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, 1f, 0))
        overlay.performClick()
        settle()
        overlay.isVisible shouldBe false
    }

    @Test
    fun drawsEveryRegion() {
        val canvas = Canvas(Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888))
        overlay.draw(canvas)
        overlay.setNavigation(LNavigation(), showOnStart = false)
        overlay.draw(canvas)
        overlay.setNavigation(DisabledNavigation(), showOnStart = false)
        overlay.draw(canvas)
        overlay.width shouldBe 100
    }
}
