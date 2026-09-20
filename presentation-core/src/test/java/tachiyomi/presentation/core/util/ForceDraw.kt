package tachiyomi.presentation.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Draws [view] (a compose host captured through `LocalView.current`) into a throwaway bitmap on the UI
 * thread, so draw-phase lambdas run regardless of whether the Robolectric window ever paints.
 */
internal fun ComposeContentTestRule.forceDraw(view: View) {
    runOnIdle {
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)
        view.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
    }
}
