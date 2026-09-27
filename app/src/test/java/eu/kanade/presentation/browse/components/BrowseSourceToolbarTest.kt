package eu.kanade.presentation.browse.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import eu.kanade.presentation.browse.UiDispatcherReset
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceToolbarTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    private fun remote(): Source = mockk(relaxed = true) { every { name } returns "Remote" }

    private fun show(
        source: Source?,
        displayMode: LibraryDisplayMode?,
        query: String? = null,
        pinned: Boolean = false,
    ) {
        compose.setContent {
            MaterialTheme {
                val behavior = TopAppBarDefaults.pinnedScrollBehavior()
                val onQuery = { query: String? -> events += "query $query" }
                if (pinned) {
                    BrowseSourceToolbar(
                        searchQuery = query,
                        onSearchQueryChange = onQuery,
                        source = source,
                        displayMode = displayMode,
                        onDisplayModeChange = { events += "mode $it" },
                        navigateUp = { events += "up" },
                        onWebViewClick = { events += "webview" },
                        onHelpClick = { events += "help" },
                        onSettingsClick = { events += "settings" },
                        onSearch = { events += "search $it" },
                        scrollBehavior = behavior,
                    )
                } else {
                    BrowseSourceToolbar(
                        searchQuery = query,
                        onSearchQueryChange = onQuery,
                        source = source,
                        displayMode = displayMode,
                        onDisplayModeChange = { events += "mode $it" },
                        navigateUp = { events += "up" },
                        onWebViewClick = { events += "webview" },
                        onHelpClick = { events += "help" },
                        onSettingsClick = { events += "settings" },
                        onSearch = { events += "search $it" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun remoteWithDisplayMode() {
        show(remote(), LibraryDisplayMode.ComfortableGrid)
        compose.onNodeWithText("Remote").assertExists()
        compose.onNodeWithContentDescription("Display mode").performClick()
        compose.onNodeWithText("List").performClick()
        compose.onNodeWithContentDescription("WebView").performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("mode List", "webview", "up")
    }

    @Test
    fun localSourceShowsHelp() {
        val local = mockkClass(LocalSource::class, relaxed = true) { every { name } returns "Local" }
        show(local, LibraryDisplayMode.List, pinned = true)
        compose.onNodeWithContentDescription("Help").performClick()
        compose.onNodeWithContentDescription("Display mode").performClick()
        compose.onNodeWithText("Compact grid").performClick()
        events shouldContainExactly listOf("help", "mode CompactGrid")
    }

    @Test
    fun configurableIsCrowded() {
        val configurable = mockk<ConfigurableSource>(relaxed = true) { every { name } returns "Conf" }
        show(configurable, LibraryDisplayMode.CompactGrid)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("WebView").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Settings").performClick()
        events shouldContainExactly listOf("webview", "settings")
    }

    @Test
    fun configurableWithoutMode() {
        val configurable = mockk<ConfigurableSource>(relaxed = true) { every { name } returns "Conf" }
        show(configurable, displayMode = null)
        compose.onNodeWithContentDescription("WebView").performClick()
        compose.onNodeWithContentDescription("Display mode").assertDoesNotExist()
        events shouldContainExactly listOf("webview")
    }

    @Test
    fun searchQueryEditing() {
        show(source = null, displayMode = null, query = "abc")
        compose.onNode(hasSetTextAction()).performTextInput("d")
        compose.onNode(hasSetTextAction()).performImeAction()
        events.last() shouldStartWith "search "
        events.first() shouldStartWith "query "
    }

    @Test
    fun simpleToolbarModes() {
        compose.setContent {
            MaterialTheme {
                BrowseSourceSimpleToolbar(
                    navigateUp = { events += "up" },
                    title = "Latest",
                    displayMode = LibraryDisplayMode.List,
                    onDisplayModeChange = { events += "mode $it" },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                )
            }
        }
        compose.onNodeWithContentDescription("Display mode").performClick()
        listOf("Comfortable grid", "Compact grid", "List").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("mode ComfortableGrid", "mode CompactGrid", "mode List", "up")
    }
}
