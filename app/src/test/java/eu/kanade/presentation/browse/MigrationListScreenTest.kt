package eu.kanade.presentation.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigratingManga
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigratingManga.SearchResult
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w500dp-h2000dp")
internal class MigrationListScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private var items by mutableStateOf(emptyList<MigratingManga>())
    private var done by mutableStateOf(false)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun migrating(id: Long): MigratingManga = MigratingManga(
        manga = Manga.create().copy(id = id, ogTitle = "Old $id"),
        chapterInfo = MigratingManga.ChapterInfo(latestChapter = 1.0, chapterCount = 1),
        sourcesString = "From $id",
        parentContext = Dispatchers.Unconfined,
    )

    private fun show() {
        compose.setContent {
            MaterialTheme {
                MigrationListScreen(
                    items = items,
                    migrationDone = done,
                    unfinishedCount = 1,
                    getManga = { Manga.create().copy(id = it.id, ogTitle = "New ${it.id}") },
                    getChapterInfo = { MigratingManga.ChapterInfo(latestChapter = 2.0, chapterCount = 2) },
                    getSourceName = { "To" },
                    onMigrationItemClick = { events += "item ${it.id}" },
                    openMigrationDialog = { events += "dialog $it" },
                    skipManga = { events += "skip $it" },
                    searchManually = { events += "manual ${it.manga.id}" },
                    migrateNow = { events += "migrate $it" },
                    copyNow = { events += "copy $it" },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun shows(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun singleSearchingEntry() {
        compose.mainClock.autoAdvance = false
        items = listOf(migrating(1L))
        show()
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.onNodeWithContentDescription("Stop").performClick()
        compose.onNodeWithText("Old 1").performClick()
        events shouldContainExactly listOf("skip 1", "item 1")
    }

    @Test
    fun finishedEntriesMigrate() {
        val found = migrating(1L).apply { searchResult.value = SearchResult.Result(7L) }
        val missing = migrating(2L).apply { searchResult.value = SearchResult.NotFound }
        items = listOf(found, missing)
        done = true
        show()
        eventually { shows("New 7") && shows("Migration (1/2)") }
        compose.onNodeWithContentDescription("Copy").performClick()
        compose.onNodeWithContentDescription("Migrate").performClick()
        compose.onNodeWithText("New 7").performClick()
        val menus = compose.onAllNodesWithContentDescription("More options")
        menus[0].performClick()
        compose.onNodeWithText("Migrate now").performClick()
        menus[0].performClick()
        compose.onNodeWithText("Copy now").performClick()
        menus[0].performClick()
        compose.onNodeWithText("Search manually").performClick()
        menus[1].performClick()
        compose.onNodeWithText("Don't migrate").performClick()
        events shouldContainExactly listOf(
            "dialog false",
            "dialog false",
            "item 7",
            "migrate 1",
            "copy 1",
            "manual 1",
            "skip 2",
        )
    }
}
