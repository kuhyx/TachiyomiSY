package mihon.feature.migration.list

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.feature.migration.list.models.MigratingManga
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MigrationListScreenContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(items: List<MigratingManga>, complete: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                MigrationListScreenContent(
                    items = items,
                    migrationComplete = complete,
                    finishedCount = 1,
                    onItemClick = { calls += "open ${it.id}" },
                    onSearchManually = { calls += "search ${it.manga.id}" },
                    onSkip = { calls += "skip $it" },
                    onMigrate = { calls += "migrate $it" },
                    onCopy = { calls += "copy $it" },
                    openMigrationDialog = { calls += "dialog $it" },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun menu(row: Int, label: String) {
        compose.onAllNodes(bareButton)[row].performClick()
        compose.waitForIdle()
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun anEmptyListHasThePlainTitle() {
        show(emptyList(), complete = false)
        compose.onNodeWithText("Migration").assertExists()
    }

    @Test
    fun rowsShowTheirState() {
        show(
            listOf(
                migrating(3L, found(30L, latestChapter = null)),
                migrating(2L, MigratingManga.SearchResult.NotFound, latestChapter = null),
                migrating(1L, latestChapter = 5.0),
            ),
        )
        compose.onNodeWithText("Migration (1/3)").assertExists()
        compose.onNodeWithText("No alternatives found").assertExists()
        compose.onNodeWithText("Target 30").assertExists()
        compose.onAllNodesWithText("Latest: Unknown").fetchSemanticsNodes().size shouldBe 2
        compose.onNodeWithText("Latest: 3").assertExists()
    }

    @Test
    fun rowsAndMatchesOpen() {
        show(listOf(migrating(3L, found(30L))))
        compose.onNodeWithText("Source 3").performClick()
        compose.onNodeWithText("Target 30").performClick()
        calls shouldContainExactly listOf("open 3", "open 30")
    }

    @Test
    fun rowActionsReachTheCallbacks() {
        show(listOf(migrating(1L), migrating(3L, found(30L))))
        compose.onAllNodes(bareButton)[0].performClick()
        menu(row = 1, label = "Search manually")
        menu(row = 1, label = "Don't migrate")
        menu(row = 1, label = "Migrate now")
        menu(row = 1, label = "Copy now")
        calls shouldContainExactly listOf("skip 1", "search 3", "skip 3", "migrate 3", "copy 3")
    }

    @Test
    fun oneItemActions() {
        show(listOf(migrating(3L, found(30L))))
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.onNodeWithContentDescription("Migrate").performClick()
        calls shouldContainExactly listOf("dialog true", "dialog false")
    }

    @Test
    fun manyItemActionsWaitForTheEnd() {
        show(listOf(migrating(1L), migrating(2L)), complete = false)
        compose.onNodeWithContentDescription("Copy").performClick()
        calls shouldContainExactly emptyList()
    }
}
