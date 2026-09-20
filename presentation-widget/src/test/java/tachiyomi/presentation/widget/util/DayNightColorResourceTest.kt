package tachiyomi.presentation.widget.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.color.DayNightColorProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.presentation.widget.R

@RunWith(RobolectricTestRunner::class)
internal class DayNightColorResourceTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun resolvesEachUiModeVariant() {
        val provider = context.dayNightColorResource(R.color.appwidget_on_secondary_container)
            .shouldBeInstanceOf<DayNightColorProvider>()

        // The v31 resource points at the dynamic light and dark palettes, which differ.
        provider.day shouldNotBe provider.night
        provider.getColor(false) shouldBe provider.day
        provider.getColor(true) shouldBe provider.night
    }

    @Test
    fun sharesOneValueWithoutNight() {
        val provider = context.dayNightColorResource(R.color.appwidget_coverscreen_background)
            .shouldBeInstanceOf<DayNightColorProvider>()

        provider.day shouldBe Color.Transparent
        provider.night shouldBe Color.Transparent
    }

    @Test
    fun keepsTheCallerConfiguration() {
        val before = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        context.dayNightColorResource(R.color.appwidget_on_secondary_container)

        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) shouldBe before
    }
}
