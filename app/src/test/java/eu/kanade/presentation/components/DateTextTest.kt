package eu.kanade.presentation.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.util.PresentationKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
internal class DateTextTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun zeroMillisIsNotApplicable() {
        compose.setContent { Text(relativeDateText(0L)) }
        compose.onNodeWithText("N/A").assertExists()
    }

    @Test
    fun millisAreRelative() {
        compose.setContent { Text(relativeDateText(System.currentTimeMillis())) }
        compose.onNodeWithText("Today").assertExists()
    }

    @Test
    fun absoluteWhenRelativeIsOff() {
        koin.uiPreferences.relativeTime.set(false)
        koin.uiPreferences.dateFormat.set("yyyy-MM-dd")
        compose.setContent { Text(relativeDateText(LocalDate.of(2020, 1, 2))) }
        compose.onNodeWithText("2020-01-02").assertExists()
    }
}
