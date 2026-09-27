package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.presentation.browse.UiDispatcherReset
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
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MigrationItemsTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val manga = Manga.create().copy(id = 1L, ogTitle = "Needle")
    private val item = MigratingManga(manga, MigratingManga.ChapterInfo(2.5, 3), "Src", Dispatchers.Unconfined)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun shows(text: String): Boolean = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    @Composable
    private fun Result(result: SearchResult, found: Manga?) {
        MigrationItemResult(
            modifier = Modifier,
            migrationItem = item,
            result = result,
            getManga = { found },
            getChapterInfo = { MigratingManga.ChapterInfo(latestChapter = null, chapterCount = 0) },
            getSourceName = { "Other ${it.id}" },
            onMigrationItemClick = { events += "migrate ${it.id}" },
        )
    }

    @Test
    fun resultStates() {
        val found = Manga.create().copy(id = 9L, ogTitle = " ")
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Result(SearchResult.NotFound, found = null)
                    Result(SearchResult.Result(9L), found = found)
                    Result(SearchResult.Result(8L), found = null)
                }
            }
        }
        compose.onNodeWithText("No Alternatives Found").assertExists()
        eventually { shows("Other 9") && shows("Latest: Unknown") }
        compose.onNodeWithText("Unknown").performScrollTo().performClick()
        events shouldContainExactly listOf("migrate 9")
    }

    @Test
    fun searchingShowsSpinner() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MaterialTheme { Result(SearchResult.Searching, found = null) } }
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithText("No Alternatives Found").assertDoesNotExist()
    }

    @Test
    fun itemShowsLatestChapter() {
        compose.setContent {
            MaterialTheme {
                MigrationItem(
                    modifier = Modifier,
                    manga = manga,
                    sourcesString = "Src",
                    chapterInfo = item.chapterInfo,
                    onClick = { events += "item" },
                )
            }
        }
        eventually { shows("Latest: 2.5") }
        compose.onNodeWithText("Needle").performClick()
        events shouldContainExactly listOf("item")
    }

    private fun showIcon(result: SearchResult) {
        compose.setContent {
            MaterialTheme {
                MigrationActionIcon(
                    modifier = Modifier,
                    result = result,
                    skipManga = { events += "skip" },
                    searchManually = { events += "manual" },
                    migrateNow = { events += "migrate" },
                    copyNow = { events += "copy" },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun menu(label: String) {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText(label).performClick()
    }

    @Test
    fun searchingIconStops() {
        showIcon(SearchResult.Searching)
        compose.onNodeWithContentDescription("Stop").performClick()
        events shouldContainExactly listOf("skip")
    }

    @Test
    fun resultIconMenu() {
        showIcon(SearchResult.Result(1L))
        listOf("Search manually", "Don't migrate", "Migrate now", "Copy now").forEach(::menu)
        events shouldContainExactly listOf("manual", "skip", "migrate", "copy")
    }

    @Test
    fun notFoundIconMenu() {
        showIcon(SearchResult.NotFound)
        menu("Search manually")
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Migrate now").assertDoesNotExist()
        compose.onNodeWithContentDescription("More options").performClick()
        events shouldContainExactly listOf("manual")
    }
}
