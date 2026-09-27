package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.manga.model.Manga

/** The feed screen's toasts, rows refreshed from the library, and browsing without a query. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SourceFeedScreenMoreTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = SourceFeedHarness()

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(true)
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        harness.start()
    }

    @After
    fun tearDown() = harness.stop()

    private fun show() {
        compose.setContent { ScreenHost(SourceFeedScreen(1L)) }
        compose.pollLabel("Popular")
    }

    // The toast is posted to the main looper from an IO coroutine; run the looper while waiting.
    private fun toasted(text: String) = compose.waitUntil(timeoutMillis = 10_000) {
        ShadowLooper.idleMainLooper()
        ShadowToast.getTextOfLatestToast() == text
    }

    @Test
    fun invalidSavedSearchToasts() {
        coEvery { harness.exhSearches.await(1L, any()) } returns listOf(harness.search(2L, "b", filters = null))
        show()
        compose.clickLabel("Filter")
        compose.pollLabel("b")
        compose.clickLabel("b")
        toasted("Saved search invalid, filters have changed")
    }

    @Test
    fun fullFeedToasts() {
        coEvery { harness.count.await(1L) } returns 11L
        show()
        compose.clickLabel("Filter")
        compose.pollLabel("A")
        compose.onAllNodes(hasText("A"), useUnmergedTree = true).onLast().performTouchInput { longClick() }
        toasted("Too many sources in your feed, cannot add more than 10")
    }

    @Test
    fun libraryCopyReplacesRow() {
        every { harness.getManga.subscribe(any<String>(), any()) } answers {
            flowOf(Manga.create().copy(id = 4L, url = firstArg(), ogTitle = "Library copy", source = 1L))
        }
        compose.setContent { ScreenHost(SourceFeedScreen(1L)) }
        compose.pollLabel("Library copy")
    }

    @Test
    fun browseWithoutQuery() {
        val navigator = mockk<Navigator>(relaxed = true)
        SourceFeedScreen(1L).onBrowseClick(navigator, sourceId = 1L)
        verify { navigator.replace(any<BrowseSourceScreen>()) }
    }
}
