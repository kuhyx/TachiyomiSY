package eu.kanade.presentation.util

import android.content.res.Resources
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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

    @Test
    fun aMissingDrawableThrows() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getDrawable(any(), any()) } returns null
        try {
            shouldThrow<Resources.NotFoundException> {
                compose.setContent { rememberResourceBitmapPainter(R.drawable.ic_book_24dp) }
                compose.waitForIdle()
            }
        } finally {
            unmockkAll()
        }
    }
}
