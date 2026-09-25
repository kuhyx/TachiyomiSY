package eu.kanade.presentation.util

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResourcesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun drawablesBecomeBitmapPainters() {
        var painter: BitmapPainter? = null
        compose.setContent { painter = rememberResourceBitmapPainter(R.drawable.ic_book_24dp) }
        compose.waitForIdle()
        painter.shouldNotBeNull()
    }
}
