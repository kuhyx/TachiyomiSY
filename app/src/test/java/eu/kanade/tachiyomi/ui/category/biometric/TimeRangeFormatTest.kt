package eu.kanade.tachiyomi.ui.category.biometric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
internal class TimeRangeFormatTest {
    @Test
    fun formatsInTheDeviceTimeFormat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TimeRange(8.hours, 17.hours + 30.minutes).getFormattedString(context) shouldBe "8:00 AM - 5:30 PM"
    }
}
