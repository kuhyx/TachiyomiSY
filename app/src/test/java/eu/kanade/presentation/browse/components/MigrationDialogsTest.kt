package eu.kanade.presentation.browse.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MigrationDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun showManga(copy: Boolean, skipped: Int) {
        compose.setContent {
            MaterialTheme {
                MigrationMangaDialog(
                    onDismissRequest = { events += "dismiss" },
                    copy = copy,
                    mangaSet = 2,
                    mangaSkipped = skipped,
                    copyManga = { events += "copy" },
                    migrateManga = { events += "migrate" },
                )
            }
        }
    }

    @Test
    fun copyWithSkipped() {
        showManga(copy = true, skipped = 1)
        compose.onNodeWithText("Copy 2 (skipping 1) entries?").assertExists()
        compose.onNodeWithText("Copy").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("copy", "dismiss")
    }

    @Test
    fun migrateWithoutSkipped() {
        showManga(copy = false, skipped = 0)
        compose.onNodeWithText("Migrate 2 entries?").assertExists()
        compose.onNodeWithText("Migrate").performClick()
        events shouldContainExactly listOf("migrate")
    }

    @Test
    fun exitDialog() {
        compose.setContent {
            MaterialTheme {
                MigrationExitDialog(onDismissRequest = { events += "dismiss" }, exitMigration = { events += "exit" })
            }
        }
        compose.onNodeWithText("Stop migrating?").assertExists()
        compose.onNodeWithText("Stop").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("exit", "dismiss")
    }

    @Test
    fun progressDialogWithProgress() {
        compose.setContent {
            MaterialTheme { MigrationProgressDialog(progress = 0.5f, exitMigration = { events += "exit" }) }
        }
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("exit")
    }

    @Test
    fun progressDialogWithoutProgress() {
        compose.setContent { MaterialTheme { MigrationProgressDialog(progress = Float.NaN, exitMigration = {}) } }
        compose.onNodeWithText("Cancel").assertExists()
    }
}
