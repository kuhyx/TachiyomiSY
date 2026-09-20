package tachiyomi.presentation.widget.components

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.layout.padding
import androidx.glance.testing.unit.hasClickAction
import androidx.glance.testing.unit.hasText
import androidx.glance.unit.ColorProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.presentation.widget.LOCKED_TEXT
import tachiyomi.presentation.widget.composeUiContext
import tachiyomi.presentation.widget.opensAnActivity

@RunWith(RobolectricTestRunner::class)
internal class LockedWidgetTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val foreground = ColorProvider(Color.White)

    @Test
    fun showsLockedNoticeOpeningTheApp() {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(200.dp, 300.dp))
            provideComposable {
                CompositionLocalProvider(composeUiContext(context)) {
                    LockedWidget(foreground = foreground)
                }
            }
            onNode(hasText(LOCKED_TEXT)).assertExists()
            onNode(hasClickAction()).assertExists()
            onNode(opensAnActivity()).assertExists()
        }
    }

    @Test
    fun keepsTheCallerModifier() {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            provideComposable {
                CompositionLocalProvider(composeUiContext(context)) {
                    LockedWidget(foreground = foreground, modifier = GlanceModifier.padding(2.dp))
                }
            }
            onNode(hasText(LOCKED_TEXT)).assertExists()
            onNode(hasClickAction()).assertExists()
        }
    }

    /**
     * Documents a defect in the main code: `stringResource` from `presentation-core` reads Compose
     * UI's `LocalContext`, which a Glance composition never provides, so the locked notice cannot
     * render in a real widget session. Delete this test once `LockedWidget` reads the string
     * through Glance's `LocalContext`.
     */
    @Test
    fun failsWithoutComposeUiContext() {
        val thrown = shouldThrow<IllegalStateException> {
            runGlanceAppWidgetUnitTest {
                setContext(context)
                provideComposable { LockedWidget(foreground = foreground) }
                awaitIdle()
            }
        }
        thrown.message shouldContain "LocalContext"
    }
}
