package tachiyomi.presentation.widget.components

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.padding
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasText
import androidx.glance.unit.ColorProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.presentation.widget.NO_RECENT_TEXT
import tachiyomi.presentation.widget.R
import tachiyomi.presentation.widget.composeUiContext
import tachiyomi.presentation.widget.coverBitmap
import tachiyomi.presentation.widget.isBitmapImage
import tachiyomi.presentation.widget.isProgressIndicator
import tachiyomi.presentation.widget.isResourceImage
import tachiyomi.presentation.widget.opensAnActivity

@RunWith(RobolectricTestRunner::class)
internal class UpdatesWidgetTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val contentColor = ColorProvider(Color.White)

    @Test
    fun showsSpinnerWhileLoading() {
        runWidget(size = DpSize(200.dp, 300.dp), data = null) {
            onNode(isProgressIndicator()).assertExists()
            onNode(hasClickAction()).assertDoesNotExist()
        }
    }

    @Test
    fun showsNoticeWithoutUpdates() {
        runWidget(size = DpSize(200.dp, 300.dp), data = emptyList()) {
            onNode(hasText(NO_RECENT_TEXT)).assertExists()
            onNode(isProgressIndicator()).assertDoesNotExist()
        }
    }

    @Test
    fun laysCoversOutInRows() {
        val bitmap = coverBitmap()
        val data = listOf(1L to bitmap, 2L to null, 3L to null, 4L to null)
        // 200 x 300 dp holds 3 columns and 3 rows: a full first row and a second one with one cover.
        runWidget(size = DpSize(200.dp, 300.dp), data = data) {
            onAllNodes(opensAnActivity()).assertCountEquals(4)
            onNode(isBitmapImage(bitmap)).assertExists()
            onAllNodes(isResourceImage(R.drawable.appwidget_cover_error)).assertCountEquals(3)
        }
    }

    @Test
    fun skipsRowsWithoutCovers() {
        val data = listOf(1L to null, 2L to null)
        // 130 x 300 dp holds 2 columns and 3 rows, so two covers fill one row and leave two empty.
        runWidget(size = DpSize(130.dp, 300.dp), data = data) {
            onAllNodes(opensAnActivity()).assertCountEquals(2)
            onAllNodes(isResourceImage(R.drawable.appwidget_cover_error)).assertCountEquals(2)
        }
    }

    @Test
    fun keepsTheCallerModifier() {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable {
                UpdatesWidget(
                    data = null,
                    contentColor = contentColor,
                    topPadding = 0.dp,
                    bottomPadding = 0.dp,
                    modifier = GlanceModifier.padding(4.dp),
                )
            }
            onNode(isProgressIndicator()).assertExists()
        }
    }

    private fun runWidget(
        size: DpSize,
        data: List<Pair<Long, Bitmap?>>?,
        assertions: GlanceAppWidgetUnitTest.() -> Unit,
    ) {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(size)
            provideComposable {
                CompositionLocalProvider(composeUiContext(context)) {
                    UpdatesWidget(data = data, contentColor = contentColor, topPadding = 0.dp, bottomPadding = 0.dp)
                }
            }
            assertions()
        }
    }
}
