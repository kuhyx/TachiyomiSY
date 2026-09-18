package eu.kanade.tachiyomi.util.system

import android.content.res.Resources
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class DensityExtensionsTest {
    @Test
    fun scalesByTheSystemDensity() {
        val density = Resources.getSystem().displayMetrics.density
        16.dpToPx shouldBe (16 * density).toInt()
        0.dpToPx shouldBe 0
    }

    @Test
    @Config(qualifiers = "xxhdpi")
    fun truncatesTowardsZero() {
        Resources.getSystem().displayMetrics.density shouldBe 3f
        7.dpToPx shouldBe 21
        (-3).dpToPx shouldBe -9
    }
}
