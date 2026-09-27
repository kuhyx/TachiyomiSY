package mihon.feature.migration.config

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.domain.migration.models.MigrationFlag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MigrationConfigScreenSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val preferences = SourcePreferences(InMemoryPreferenceStore())
    private val started = mutableListOf<String?>()
    private var dismissed = 0

    private fun show() {
        compose.setContent {
            MaterialTheme {
                MigrationConfigScreenSheet(
                    preferences = preferences,
                    onDismissRequest = { dismissed++ },
                    onStartMigration = { started += it },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun click(label: String) {
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun chipsToggleTheirFlag() {
        show()
        click("Chapters")
        preferences.migrationFlags.get().contains(MigrationFlag.CHAPTER) shouldBe false
        click("Chapters")
        preferences.migrationFlags.get().contains(MigrationFlag.CHAPTER) shouldBe true
        click("Notes")
        preferences.migrationFlags.get().contains(MigrationFlag.NOTES) shouldBe false
    }

    @Test
    fun removeDownloadsSwitchToggles() {
        show()
        click("Delete downloads of current entry after migration")
        preferences.migrationFlags.get().contains(MigrationFlag.REMOVE_DOWNLOAD) shouldBe false
        click("Delete downloads of current entry after migration")
        preferences.migrationFlags.get().contains(MigrationFlag.REMOVE_DOWNLOAD) shouldBe true
    }

    @Test
    fun listSwitchesTogglePreferences() {
        show()
        click("Hide entries without a match")
        click("Hide entries without newer chapters")
        click("Advanced search mode")
        click("Match based on chapter number")
        preferences.migrationHideUnmatched.get() shouldBe true
        preferences.migrationHideWithoutUpdates.get() shouldBe true
        preferences.migrationDeepSearchMode.get() shouldBe true
        preferences.migrationPrioritizeByChapters.get() shouldBe true
        compose.onNodeWithText("Only show entry if the match has additional chapters").assertExists()
    }

    @Test
    fun blankKeywordsStartWithNull() {
        show()
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        started shouldContainExactly listOf(null)
        dismissed shouldBe 0
    }

    @Test
    fun keywordsAreTrimmed() {
        show()
        compose.onNodeWithText("Additional keywords (optional)").performTextInput("  more words  ")
        compose.waitForIdle()
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        started shouldContainExactly listOf("more words")
    }

    @Test
    fun uncheckedChipsStayUnchecked() {
        preferences.migrationFlags.set(emptySet())
        show()
        click("Categories")
        preferences.migrationFlags.get() shouldContainExactly setOf(MigrationFlag.CATEGORY)
    }
}
