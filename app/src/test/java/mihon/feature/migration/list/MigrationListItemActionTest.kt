package mihon.feature.migration.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import mihon.feature.migration.list.models.MigratingManga
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MigrationListItemActionTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(result: MigratingManga.SearchResult) {
        compose.setContent {
            MaterialTheme {
                MigrationListItemAction(
                    modifier = Modifier,
                    result = result,
                    onSearchManually = { calls += "search" },
                    onSkip = { calls += "skip" },
                    onMigrate = { calls += "migrate" },
                    onCopy = { calls += "copy" },
                )
            }
        }
    }

    private fun menu(label: String) {
        compose.onNode(bareButton).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun searchingCanBeSkipped() {
        show(MigratingManga.SearchResult.Searching)
        compose.onNode(bareButton).performClick()
        calls shouldContainExactly listOf("skip")
    }

    @Test
    fun notFoundOffersSearchAndSkip() {
        show(MigratingManga.SearchResult.NotFound)
        menu("Search manually")
        compose.onNodeWithText("Migrate now").assertDoesNotExist()
        menu("Don't migrate")
        calls shouldContainExactly listOf("search", "skip")
    }

    @Test
    fun aMatchOffersMigrateAndCopy() {
        show(found(2L))
        menu("Migrate now")
        menu("Copy now")
        calls shouldContainExactly listOf("migrate", "copy")
    }
}
