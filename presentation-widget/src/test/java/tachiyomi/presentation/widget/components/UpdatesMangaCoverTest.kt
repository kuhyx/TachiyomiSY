package tachiyomi.presentation.widget.components

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasClickAction
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.presentation.widget.R
import tachiyomi.presentation.widget.coverBitmap
import tachiyomi.presentation.widget.isBitmapImage
import tachiyomi.presentation.widget.isResourceImage

@RunWith(RobolectricTestRunner::class)
internal class UpdatesMangaCoverTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun drawsTheBitmapAtCoverSize() {
        val bitmap = coverBitmap()
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { UpdatesMangaCover(cover = bitmap) }
            onNode(isBitmapImage(bitmap)).assertExists()
            onNode(isResourceImage(R.drawable.appwidget_cover_error)).assertDoesNotExist()
            onNode(hasClickAction()).assertDoesNotExist()
        }
        CoverWidth shouldBe 58.dp
        CoverHeight shouldBe 87.dp
    }

    @Test
    fun drawsPlaceholderWithoutBitmap() {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable { UpdatesMangaCover(cover = null) }
            onNode(isResourceImage(R.drawable.appwidget_cover_error)).assertExists()
        }
    }

    @Test
    fun appliesTheCallerModifier() {
        val intent = Intent(context, MainActivity::class.java)
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable {
                UpdatesMangaCover(cover = null, modifier = GlanceModifier.clickable(actionStartActivity(intent)))
            }
            onNode(hasClickAction()).assertExists()
        }
    }
}
