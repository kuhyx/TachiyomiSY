package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val MARKDOWN = """# Title
Some *text* with <b>html</b> and ![pic](http://x/p.png "Pic") and ![bare](http://x/b.png).

1. one
2. two

- a
    - b

---

| h1 | h2 |
|----|----|
| c1 | c2 |
"""

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class MangaDescriptionTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    private var args by mutableStateOf(Args())
    private var shown = false

    private data class Args(
        val expanded: Boolean = false,
        val description: String? = "A story",
        val tags: List<String>? = listOf("Action", "Drama"),
        val notes: String = "",
        val chips: SearchMetadataChips? = null,
    )

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(next: Args = Args()) {
        args = next
        if (!shown) {
            shown = true
            compose.setContent {
                MaterialTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        key(args.expanded) { Description(args) }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Description(current: Args) {
        ExpandableMangaDescription(
            defaultExpandState = current.expanded,
            description = current.description,
            tagsProvider = { current.tags },
            notes = current.notes,
            onTagSearch = { events += "tag $it" },
            onCopyTagToClipboard = { events += "copy $it" },
            onEditNotes = { events += "notes" },
            searchMetadataChips = current.chips,
            doSearch = { query, global -> events += "search $query $global" },
        )
    }

    @Test
    fun collapsedTagMenu() {
        show()
        listOf("Search", "Global search", "Copy to clipboard").forEach {
            compose.onNodeWithText("Drama").performClick()
            compose.onNodeWithText(it).performClick()
        }
        events shouldContainExactly listOf("tag Drama", "search Drama true", "copy Drama")
    }

    @Test
    fun expandedTagsWrap() {
        show(Args(expanded = true))
        compose.onNodeWithText("Action").performClick()
        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithContentDescription("Less").assertExists()
        events shouldContainExactly listOf("tag Action")
    }

    @Test
    fun namespacedChips() {
        val chips = SearchMetadataChips(null, 1L, listOf("female:glasses", "misc:tall"))
        show(Args(expanded = true, tags = listOf("female:glasses", "misc:tall"), chips = chips))
        compose.onNodeWithText("glasses").performClick()
        compose.onNodeWithText("Search").performClick()
        events shouldContainExactly listOf("tag female:glasses")
    }

    @Test
    fun blankDescriptionNoTags() {
        show(Args(description = " ", tags = null))
        compose.onNodeWithText("No description").assertExists()
        compose.onNodeWithText("Action").assertDoesNotExist()
        show(Args(description = null, tags = emptyList()))
        compose.onNodeWithText("No description").performClick()
        compose.onNodeWithContentDescription("Less").assertExists()
        compose.onNodeWithText("No description").performClick()
        compose.onNodeWithContentDescription("More").assertExists()
    }

    @Test
    fun notesEditButton() {
        show(Args(expanded = true, notes = "My **note**"))
        compose.onNodeWithText("Edit notes").performClick()
        compose.onNodeWithText("No description").assertDoesNotExist()
        events shouldContainExactly listOf("notes")
        show(Args(notes = "Other"))
        compose.onNodeWithText("Edit notes").assertDoesNotExist()
    }

    @Test
    fun markdownWithoutImages() {
        koin.uiPreferences.imagesInDescription.set(false)
        show(Args(expanded = true, description = MARKDOWN))
        compose.onNodeWithText("Title", substring = true).assertExists()
    }

    @Test
    fun markdownWithImages() {
        koin.uiPreferences.imagesInDescription.set(true)
        show(Args(expanded = true, description = MARKDOWN))
        compose.onNodeWithText("Title", substring = true).assertExists()
    }

    @Test
    fun notesPreview() {
        compose.setContent { MaterialTheme { MangaNotesSectionPreview() } }
        compose.onNodeWithText("Edit notes").assertExists()
    }
}
