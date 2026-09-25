package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
internal class MangaDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun deleteChaptersConfirms() {
        compose.setContent {
            MaterialTheme {
                DeleteChaptersDialog(onDismissRequest = { events += "dismiss" }, onConfirm = { events += "ok" })
            }
        }
        compose.onNodeWithText("Are you sure?").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss", "ok", "dismiss")
    }

    @Test
    fun intervalWithAPrediction() {
        val next = Instant.now().plus(3, ChronoUnit.DAYS).plusSeconds(60)
        compose.setContent {
            MaterialTheme {
                SetIntervalDialog(interval = 7, nextUpdate = next, onDismissRequest = { events += "dismiss" })
            }
        }
        compose.onNodeWithText("in around 3 days", substring = true).assertExists()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun customIntervalNoPrediction() {
        compose.setContent {
            MaterialTheme {
                SetIntervalDialog(
                    interval = -3,
                    nextUpdate = null,
                    onDismissRequest = { events += "dismiss" },
                    onValueChanged = { events += "value $it" },
                )
            }
        }
        compose.onNodeWithText("This manga is either completed", substring = true).assertExists()
        compose.onNodeWithText("Custom update frequency:").assertExists()
        compose.onNodeWithText("3").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events.first().startsWith("value ") shouldBe true
    }

    @Test
    fun negativePredictionIsUnknown() {
        compose.setContent {
            MaterialTheme {
                SetIntervalDialog(interval = -1, nextUpdate = Instant.now(), onDismissRequest = {})
            }
        }
        compose.onNodeWithText("This manga is either completed", substring = true).assertExists()
    }

    @Test
    fun releaseBuildsHideThePicker() {
        mockkStatic("eu.kanade.tachiyomi.util.system.BuildConfigKt")
        every { isDebugBuildType } returns false
        compose.setContent {
            MaterialTheme {
                SetIntervalDialog(interval = 0, nextUpdate = null, onDismissRequest = {}, onValueChanged = {})
            }
        }
        compose.onNodeWithText("Custom update frequency:").assertDoesNotExist()
    }
}
