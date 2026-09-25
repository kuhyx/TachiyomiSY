package exh.debug

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import cafe.adriel.voyager.navigator.Navigator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.domain.manga.interactor.GetAllManga
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetSearchMetadata
import tachiyomi.domain.manga.model.Manga

private const val SLOW_MILLIS = 300L
private const val SLOW_RESULT = 7

// Two section headers, one divider and the trailing spacer surround the menu's rows.
private const val EXTRA_ROWS = 3

/** A menu entry of this test's own, so that the listing can be slow without touching a real function. */
internal object SlowFunctions {
    fun slowCount(): Int = SLOW_RESULT
}

@RunWith(RobolectricTestRunner::class)
internal class SettingsDebugScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val getAllManga = mockk<GetAllManga>()

    // The overlay toggle is stored as the opposite of its default, so its row is marked MODIFIED.
    private val store = InMemoryPreferenceStore(
        sequenceOf(
            InMemoryPreferenceStore.InMemoryPreference(
                key = "eh_debug_toggle_enable_debug_overlay",
                data = false,
                defaultValue = true,
            ),
        ),
    )
    private val getSearchMetadata = mockk<GetSearchMetadata>()

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single<PreferenceStore> { store }
                    single { mockk<Database> { every { ehQueries } returns mockk<EhQueries>(relaxed = true) } }
                    single { mockk<GetFavorites>() }
                    single { getSearchMetadata }
                    single { getAllManga }
                },
            )
        }
        coEvery { getAllManga.await() } returns listOf(Manga.create().copy(id = 1))
        coEvery { getSearchMetadata.await() } throws IllegalStateException("no metadata table")
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun showScreen() {
        compose.setContent { MaterialTheme { Navigator(SettingsDebugScreen()) } }
        compose.waitUntil(timeoutMillis = 10_000) { nodesWithText("Functions") == 1 }
    }

    private fun nodesWithText(text: String, substring: Boolean = false): Int =
        compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().size

    // The menu is a lazy column, so a row must be scrolled into view before it exists.
    private fun scrollTo(text: String) {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(text))
    }

    @Test
    fun loadingComesFirst() {
        mockkObject(DebugFunctions)
        every { DebugFunctions.entries() } answers {
            Thread.sleep(SLOW_MILLIS)
            listOf(DebugFunctions.Entry(SlowFunctions, SlowFunctions::slowCount))
        }
        // The slow listing means the first composition shows the loading screen.
        compose.setContent { MaterialTheme { Navigator(SettingsDebugScreen()) } }
        compose.waitUntil(timeoutMillis = 20_000) { nodesWithText("Slow count") == 1 }
        // A running function dims the menu until its result arrives.
    }

    @Test
    fun togglesFlipAndShowChanges() {
        // The toggles resolve their store once per JVM, so which store is live depends on test order.
        val overlay = DebugToggles.ENABLE_DEBUG_OVERLAY
        overlay.enabled = !overlay.default
        val modifiedRows = if (overlay.enabled != overlay.default) 1 else 0
        showScreen()
        scrollTo("Enable debug overlay")
        nodesWithText("MODIFIED") shouldBe modifiedRows
        compose.onNodeWithText("Enable exh root redirect").performClick()
        compose.waitForIdle()
        scrollTo("Include only root when loading exh versions")
        compose.onNodeWithText("Include only root when loading exh versions").assertIsDisplayed()
        // The list ends with a spacer for the navigation bar, past the last toggle.
        val rows = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        rows.performScrollToIndex(DebugFunctions.entries().size + DebugToggles.entries.size + EXTRA_ROWS)
    }

    @Test
    fun listsFunctionsAndToggles() {
        showScreen()
        compose.onNodeWithText("DEBUG MENU").assertIsDisplayed()
        compose.onNodeWithText("Functions").assertIsDisplayed()
        scrollTo("Count manga in database")
        compose.onNodeWithText("Count manga in database").assertIsDisplayed()
        scrollTo("Toggles")
        compose.onNodeWithText("Toggles").assertIsDisplayed()
        scrollTo("Enable exh root redirect")
        compose.onNodeWithText("Enable exh root redirect").assertIsDisplayed()
    }

    @Test
    fun functionResultsOpenADialog() {
        showScreen()
        scrollTo("Count manga in database")
        compose.onNodeWithText("Count manga in database").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { nodesWithText("Function returned result:\n\n1") == 1 }
        compose.onNodeWithText("Function returned result:\n\n1").assertIsDisplayed()
        // The dialog repeats the function's name as its title.
        nodesWithText("Count manga in database") shouldBe 2
    }

    /**
     * A function that throws is reported as its stack trace. Driven through the private handler
     * because the click's coroutine dies with the exception before it can set the dialog state.
     */
    @Test
    fun failingFunctionsShowTheTrace() {
        val entry = DebugFunctions.entries().first { it.function.name == "countMetadataInDatabase" }
        val handler = SettingsDebugScreen::class.java
            .getDeclaredMethod("runDebugFunction", DebugFunctions.Entry::class.java)
            .also { it.isAccessible = true }
        val text = handler.invoke(SettingsDebugScreen(), entry) as String
        text shouldStartWith "Function threw exception:"
        text shouldContain "no metadata table"
    }
}
