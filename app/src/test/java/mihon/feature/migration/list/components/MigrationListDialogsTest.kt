package mihon.feature.migration.list.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MigrationListDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private var confirmed = 0

    private fun mangaDialog(copy: Boolean, skipped: Int) {
        compose.setContent {
            MaterialTheme {
                MigrationMangaDialog(
                    onDismissRequest = { dismissed++ },
                    copy = copy,
                    totalCount = 3,
                    skippedCount = skipped,
                    onMigrate = { confirmed++ },
                )
            }
        }
    }

    @Test
    fun theExitDialog() {
        compose.setContent {
            MaterialTheme { MigrationExitDialog(onDismissRequest = { dismissed++ }, exitMigration = { confirmed++ }) }
        }
        compose.onNodeWithText("Stop migrating?").assertExists()
        compose.onNodeWithText("Stop").performClick()
        compose.onNodeWithText("Cancel").performClick()
        confirmed shouldBe 1
        dismissed shouldBe 1
    }

    @Test
    fun migratingWithSkips() {
        mangaDialog(copy = false, skipped = 2)
        compose.onNodeWithText("Migrate 3 entries?").assertExists()
        compose.onNodeWithText("2 entries were skipped").assertExists()
        compose.onNodeWithText("Migrate").performClick()
        compose.onNodeWithText("Cancel").performClick()
        confirmed shouldBe 1
        dismissed shouldBe 1
    }

    @Test
    fun copyingWithoutSkips() {
        mangaDialog(copy = true, skipped = 0)
        compose.onNodeWithText("Copy 3 entries?").assertExists()
        compose.onNodeWithText("entries were skipped", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Copy").performClick()
        confirmed shouldBe 1
    }

    @Test
    fun progressCanBeCancelled() {
        compose.setContent {
            MaterialTheme { MigrationProgressDialog(progress = 0.5F, exitMigration = { confirmed++ }) }
        }
        compose.onNodeWithText("Cancel").performClick()
        confirmed shouldBe 1
    }

    @Test
    fun unknownProgressHasNoBar() {
        compose.setContent { MaterialTheme { MigrationProgressDialog(progress = Float.NaN, exitMigration = {}) } }
        compose.onNodeWithText("Cancel").assertExists()
    }
}
