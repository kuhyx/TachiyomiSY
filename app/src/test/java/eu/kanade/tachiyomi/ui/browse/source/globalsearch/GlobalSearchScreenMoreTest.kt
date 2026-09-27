package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.labelShown
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

/** When the screen skips straight to a single hit, and rows refreshed from the library. */
@RunWith(RobolectricTestRunner::class)
internal class GlobalSearchScreenMoreTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SearchHarness()

    @Before
    fun setUp() {
        resetUiDispatcher()
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(true)
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        harness.start()
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(screen: GlobalSearchScreen) = compose.setContent { ScreenHost(screen) }

    @Test
    fun singleHitWaitsForResult() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L)))
        harness.installed.value = listOf(ext)
        harness.gates[5L] = CompletableDeferred()
        show(GlobalSearchScreen("q", ext.pkgName))
        coVerify(timeout = 5_000) { harness.catalogue[0].getSearchManga(1, "q", any()) }
        compose.waitForIdle()
        compose.labelShown("S5") shouldBe false
        harness.gates.getValue(5L).complete(Unit)
        compose.pollLabel("opened:MangaScreen")
    }

    @Test
    fun blankQueryShowsTheList() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L)))
        harness.installed.value = listOf(ext)
        show(GlobalSearchScreen("", ext.pkgName))
        // Nothing to search for: the list screen with its filter chips, not the single-hit spinner.
        compose.pollLabel("Has results")
        compose.labelShown("opened:MangaScreen") shouldBe false
    }

    @Test
    fun emptyFilterShowsTheList() {
        harness.source(1L)
        show(GlobalSearchScreen("q", ""))
        compose.pollLabel("T1")
    }

    @Test
    fun twoSourcesShowTheList() {
        val ext = installed("Ext").copy(sources = listOf(harness.source(5L), harness.source(6L)))
        harness.installed.value = listOf(ext)
        show(GlobalSearchScreen("q", ext.pkgName))
        compose.pollLabel("T6")
    }

    @Test
    fun libraryCopyReplacesTitle() {
        every { harness.getManga.subscribe(any<String>(), any()) } answers {
            flowOf(null, Manga.create().copy(id = 4L, url = firstArg(), ogTitle = "Library copy", source = 1L))
        }
        // The screen starts on pinned sources only.
        harness.koin.sourcePreferences.pinnedSources.set(setOf("1"))
        harness.source(1L)
        show(GlobalSearchScreen("q"))
        compose.pollLabel("Library copy")
    }
}
