package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class LayoutsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aroundLayoutPlacesAllThree() {
        compose.setContent {
            AroundLayout(
                startLayout = { Box(Modifier.size(10.dp)) { Text("start") } },
                endLayout = { Box(Modifier.size(12.dp)) { Text("end") } },
                content = { Text("body") },
                modifier = Modifier,
            )
        }
        compose.onNodeWithText("body").assertExists()
        compose.onNodeWithText("start").assertExists()
        compose.onNodeWithText("end").assertExists()
    }

    @Test
    fun aroundLayoutWithEmptySides() {
        compose.setContent { AroundLayout(startLayout = {}, endLayout = {}, content = { Text("only") }) }
        compose.onNodeWithText("only").assertExists()
    }

    @Test
    fun everyBannerShows() {
        compose.setContent {
            MaterialTheme {
                AppStateBanners(downloadedOnlyMode = true, incognitoMode = true, indexing = true)
                WarningBanner(textRes = MR.strings.empty_screen, modifier = Modifier)
            }
        }
        compose.onNodeWithText("Downloaded only").assertExists()
        compose.onNodeWithText("Incognito mode").assertExists()
        compose.onNodeWithText("Checking downloads").assertExists()
        compose.onNodeWithText("Well, this is awkward").assertExists()
    }

    @Test
    fun noBannersWhenAllOff() {
        compose.setContent {
            MaterialTheme {
                AppStateBanners(downloadedOnlyMode = false, incognitoMode = false, indexing = false, modifier = Modifier)
                WarningBanner(textRes = MR.strings.empty_screen)
            }
        }
        compose.onNodeWithText("Downloaded only").assertDoesNotExist()
        compose.onNodeWithText("Incognito mode").assertDoesNotExist()
    }
}
