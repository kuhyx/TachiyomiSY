package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTouchInput
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The global search screen: waiting for sources, the result rows, and jumping straight to a single hit. */
@RunWith(RobolectricTestRunner::class)
internal class GlobalSearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SearchHarness()
    private val loaded = MutableStateFlow(true)

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns loaded
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        // The screen starts on pinned sources only.
        harness.koin.sourcePreferences.pinnedSources.set(setOf("1", "5"))
        harness.start()
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(screen: GlobalSearchScreen) = compose.setContent { ScreenHost(screen) }

    @Test
    fun waitsForSources() {
        harness.source(1L)
        loaded.value = false
        show(GlobalSearchScreen("q"))
        compose.waitForIdle()
        compose.labelShown("S1") shouldBe false
        loaded.value = true
        compose.pollLabel("T1")
    }

    @Test
    fun resultsOpenTheirScreens() {
        harness.source(1L)
        show(GlobalSearchScreen("q"))
        compose.pollLabel("T1")
        compose.clickLabel("T1")
        compose.pollLabel("opened:MangaScreen")
    }

    @Test
    fun longPressOpensManga() {
        harness.source(1L)
        show(GlobalSearchScreen("q"))
        compose.pollLabel("T1")
        compose.onAllNodes(hasText("T1")).onLast().performTouchInput { longClick() }
        compose.pollLabel("opened:MangaScreen")
    }

    @Test
    fun sourceRowBrowses() {
        harness.source(1L)
        show(GlobalSearchScreen("q"))
        compose.pollLabel("S1")
        compose.clickLabel("S1")
        compose.pollLabel("opened:BrowseSourceScreen")
    }

    @Test
    fun singleHitOpensDirectly() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L)))
        harness.installed.value = listOf(ext)
        show(GlobalSearchScreen("q", ext.pkgName))
        compose.pollLabel("opened:MangaScreen")
    }

    @Test
    fun severalHitsShowTheList() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L, titles = listOf("A", "B"))))
        harness.installed.value = listOf(ext)
        show(GlobalSearchScreen("q", ext.pkgName))
        compose.pollLabel("A")
    }

    @Test
    fun failedHitShowsTheList() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L, titles = null)))
        harness.installed.value = listOf(ext)
        show(GlobalSearchScreen("q", ext.pkgName))
        compose.pollLabel("S5")
    }
}
