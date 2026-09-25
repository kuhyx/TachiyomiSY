package eu.kanade.tachiyomi.util.system

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import coil3.size.Scale
import coil3.size.ScaleDrawable
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DrawableExtensionsTest {

    @Test
    fun readsTheBitmapOfEachDrawable() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        BitmapDrawable(null, bitmap).getBitmapOrNull() shouldBe bitmap
        val scaled = ScaleDrawable(BitmapDrawable(null, bitmap), Scale.FIT)
        scaled.getBitmapOrNull() shouldBe bitmap
        val shape = ShapeDrawable(OvalShape()).apply {
            intrinsicWidth = 2
            intrinsicHeight = 2
        }
        ScaleDrawable(child = shape, scale = Scale.FILL).getBitmapOrNull().shouldNotBeNull()
        ColorDrawable(0).getBitmapOrNull().shouldBeNull()
    }
}
