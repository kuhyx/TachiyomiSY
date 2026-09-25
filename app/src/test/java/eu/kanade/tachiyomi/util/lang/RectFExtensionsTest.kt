package eu.kanade.tachiyomi.util.lang

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences.TappingInvertMode
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RectFExtensionsTest {

    private val rect = RectF(0.1f, 0.2f, 0.4f, 0.7f)

    @Test
    fun invertsPerMode() {
        rect.invert(TappingInvertMode.NONE) shouldBe rect
        rect.invert(TappingInvertMode.HORIZONTAL) shouldBe RectF(0.6f, 0.2f, 0.9f, 0.7f)
        rect.invert(TappingInvertMode.VERTICAL) shouldBe RectF(0.1f, 0.3f, 0.4f, 0.8f)
        rect.invert(TappingInvertMode.BOTH) shouldBe RectF(0.6f, 0.3f, 0.9f, 0.8f)
    }
}
