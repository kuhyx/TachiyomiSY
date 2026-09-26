package exh.debug

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
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
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.domain.manga.interactor.GetAllManga
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetSearchMetadata
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.declaredFunctions

private const val SLOW_MILLIS = 300L
private const val SLOW_RESULT = 7
private const val WAIT_MILLIS = 20_000L

// Two section headers, one divider and the trailing spacer surround the menu's rows.
private const val EXTRA_ROWS = 3

/** A menu entry of this test's own, so that the listing can be slow without touching a real function. */
internal object SlowFunctions {
    /** Opened by the test once it has seen the running overlay. */
    var gate: CountDownLatch = CountDownLatch(0)

    /** Counted down when a call has started, i.e. after the menu marked itself running. */
    var entered: CountDownLatch = CountDownLatch(0)

    fun slowCount(): Int {
        entered.countDown()
        gate.await(WAIT_MILLIS, TimeUnit.MILLISECONDS)
        return SLOW_RESULT
    }
}

@RunWith(RobolectricTestRunner::class)
internal class SettingsDebugScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val getAllManga = mockk<GetAllManga>()

    // A store whose preferences emit their changes, so a toggle row follows its own writes.
    private val store = MapPreferenceStore()
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
            listOf(DebugFunctions.Entry(SlowFunctions, SlowFunctions::class.declaredFunctions.single()))
        }
        // The slow listing means the first composition shows the loading screen.
        compose.setContent { MaterialTheme { Navigator(SettingsDebugScreen()) } }
        compose.waitUntil(timeoutMillis = 20_000) { nodesWithText("Slow count") == 1 }
        // A running function dims the menu until its result arrives.
        SlowFunctions.gate = CountDownLatch(1)
        compose.onNodeWithText("Slow count").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { progressIndicators() == 1 }
        SlowFunctions.gate.countDown()
        compose.waitUntil(timeoutMillis = 10_000) { nodesWithText("Function returned result:\n\n7") == 1 }
        compose.waitUntil(timeoutMillis = 10_000) { progressIndicators() == 0 }
        // Run again behind the open dialog (a semantics click skips its touch barrier): no dimming while it shows.
        SlowFunctions.gate = CountDownLatch(1)
        SlowFunctions.entered = CountDownLatch(1)
        compose.onAllNodes(hasText("Slow count") and hasClickAction()).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        SlowFunctions.entered.await(WAIT_MILLIS, TimeUnit.MILLISECONDS) shouldBe true
        compose.waitForIdle()
        progressIndicators() shouldBe 0
        SlowFunctions.gate.countDown()
    }

    private fun progressIndicators(): Int =
        compose.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).fetchSemanticsNodes().size

    @Test
    fun togglesFlipAndShowChanges() {
        // The overlay toggle is stored as the opposite of its default, so its row is marked MODIFIED.
        val overlay = DebugToggles.ENABLE_DEBUG_OVERLAY
        overlay.enabled = !overlay.default
        val modifiedRows = 1
        showScreen()
        scrollTo("Enable debug overlay")
        nodesWithText("MODIFIED") shouldBe modifiedRows
        // Flipped and flipped back, so the switch turns both ways; the row's MODIFIED mark shows each write.
        compose.onNodeWithText("Enable exh root redirect").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { nodesWithText("MODIFIED") == modifiedRows + 1 }
        compose.onNodeWithText("Enable exh root redirect").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { nodesWithText("MODIFIED") == modifiedRows }
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
