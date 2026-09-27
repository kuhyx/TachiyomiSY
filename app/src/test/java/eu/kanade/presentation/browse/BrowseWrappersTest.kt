package eu.kanade.presentation.browse

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class BrowseWrappersTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    private val tab = TabContent(
        titleRes = MR.strings.label_sources,
        actions = listOf(AppBar.Action(title = "Star", icon = Icons.Outlined.Star, onClick = { events += "star" })),
    ) { _, _ -> Text("tab body") }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun wrapperWithBack() {
        compose.setContent { MaterialTheme { BrowseTabWrapper(tab, onBackPressed = { events += "back" }) } }
        compose.onNodeWithText("tab body").assertExists()
        compose.onNodeWithContentDescription("Star").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("star", "back")
    }

    @Test
    fun wrapperWithoutBack() {
        compose.setContent { MaterialTheme { BrowseTabWrapper(tab) } }
        compose.onNodeWithText("Sources").assertExists()
        compose.onNodeWithContentDescription("Navigate up").assertDoesNotExist()
    }

    @Test
    fun migrateSearchForwards() {
        val manga = Manga.create().copy(id = 5L, ogTitle = "Found")
        val state = SearchScreenModel.State(
            searchQuery = "q",
            items = mapOf(browseSource(2L) to SearchItemResult.Success(listOf(manga))),
        )
        compose.setContent {
            MaterialTheme {
                MigrateSearchScreen(
                    state = state,
                    fromSourceId = 2L,
                    navigateUp = { events += "up" },
                    onChangeSearchQuery = { events += "query $it" },
                    onSearch = { events += "search $it" },
                    onChangeSearchFilter = { events += "filter $it" },
                    onToggleResults = { events += "toggle" },
                    onClickSource = { events += "source ${it.id}" },
                    onClickItem = { events += "item ${it.id}" },
                    onLongClickItem = { events += "long ${it.id}" },
                    getManga = { remember(it) { mutableStateOf(it) } },
                )
            }
        }
        compose.onNodeWithText("▶ Source 2").performClick()
        compose.onNodeWithText("Found").performClick()
        compose.onNodeWithText("Has results").performClick()
        compose.onNodeWithText("Pinned").assertDoesNotExist()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("source 2", "item 5", "toggle", "up")
    }
}
