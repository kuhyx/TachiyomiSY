package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class OpenSourceLicensesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun listsTheBundledLibraries() {
        compose.setContent { ScreenHost(OpenSourceLicensesScreen()) }
        compose.waitForLabel("Open source licenses")
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        compose.hasLabel("Open source licenses") shouldBe true
    }
}
