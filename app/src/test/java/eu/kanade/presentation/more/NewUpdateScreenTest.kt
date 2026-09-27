package eu.kanade.presentation.more

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.tachiyomi.ui.library.waitForLabel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NewUpdateScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buttonsReachTheirCallbacks() {
        val calls = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                NewUpdateScreen(
                    versionName = "v1.2.3",
                    changelogInfo = "## Changes\n- Fixed",
                    onOpenInBrowser = { calls += "browser" },
                    onRejectUpdate = { calls += "reject" },
                    onAcceptUpdate = { calls += "accept" },
                )
            }
        }
        compose.waitForLabel("v1.2.3")
        compose.onNodeWithText("Open on GitHub").performScrollTo().performClick()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Download").performClick()
        calls shouldContainExactly listOf("browser", "reject", "accept")
    }

    @Test
    fun previewRenders() {
        compose.setContent { NewUpdateScreenPreview() }
        compose.waitForLabel("v0.99.9")
        compose.waitForLabel("New version available!")
    }
}
