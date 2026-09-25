package eu.kanade.presentation.updates

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.ui.updates.UpdatesSettingsScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.updates.service.UpdatesPreferences

@RunWith(RobolectricTestRunner::class)
internal class UpdatesDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun deleteConfirmation() {
        compose.setContent {
            MaterialTheme {
                UpdatesDeleteConfirmDialog(onDismissRequest = { events += "dismiss" }, onConfirm = { events += "ok" })
            }
        }
        compose.onNodeWithText("Are you sure you want to delete the selected chapters?").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("ok", "dismiss", "dismiss")
    }

    @Test
    fun filterDialogTogglesEachFilter() {
        val preferences = UpdatesPreferences(FlowPreferenceStore())
        compose.setContent {
            MaterialTheme {
                UpdatesFilterDialog(
                    onDismissRequest = { events += "dismiss" },
                    screenModel = UpdatesSettingsScreenModel(preferences),
                )
            }
        }
        compose.onNodeWithText("Downloaded").performClick()
        compose.onNodeWithText("Unread").performClick()
        compose.onNodeWithText("Started").performClick()
        compose.onNodeWithText("Bookmarked").performClick()
        compose.waitForIdle()
        preferences.filterDownloaded.get() shouldBe TriState.ENABLED_IS
        preferences.filterUnread.get() shouldBe TriState.ENABLED_IS
        preferences.filterStarted.get() shouldBe TriState.ENABLED_IS
        preferences.filterBookmarked.get() shouldBe TriState.ENABLED_IS
    }

    @Test
    fun scanlatorSwitchAndRowToggle() {
        val preferences = UpdatesPreferences(FlowPreferenceStore())
        val initial = preferences.filterExcludedScanlators.get()
        compose.setContent {
            MaterialTheme {
                UpdatesFilterDialog(onDismissRequest = {}, screenModel = UpdatesSettingsScreenModel(preferences))
            }
        }
        compose.onNodeWithText("Filter excluded scanlators").performClick()
        compose.waitForIdle()
        preferences.filterExcludedScanlators.get() shouldBe !initial
        val switch = compose.onNode(isToggleable())
        if (initial) switch.assertIsOff() else switch.assertIsOn()
        switch.performClick()
        compose.waitForIdle()
        preferences.filterExcludedScanlators.get() shouldBe initial
    }
}
