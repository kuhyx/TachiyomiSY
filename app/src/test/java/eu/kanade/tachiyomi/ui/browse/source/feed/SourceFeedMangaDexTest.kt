package eu.kanade.tachiyomi.ui.browse.source.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.clickLabel
import eu.kanade.tachiyomi.ui.base.pollLabel
import eu.kanade.tachiyomi.ui.base.resetUiDispatcher
import exh.source.mangaDexSourceIds
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The feed of a MangaDex source without filters: its random pick opens the picked manga's browse. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SourceFeedMangaDexTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = SourceFeedHarness()
    private val previousMangaDex = mangaDexSourceIds
    private val dex = mockk<MangaDex>(relaxed = true) {
        every { id } returns 1L
        every { name } returns "Dex"
        every { lang } returns "en"
        every { supportsLatest } returns false
        every { getFilterList() } returns FilterList()
        coEvery { getPopularManga(1) } returns MangasPage(
            listOf(
                SManga.create().apply {
                    url = "/pop"
                    title = "Dex manga"
                },
            ),
            false,
        )
        coEvery { fetchRandomMangaUrl() } returns "/random"
    }

    @Before
    fun setUp() {
        resetUiDispatcher()
        mangaDexSourceIds = listOf(1L)
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(true)
        every { harness.sourceManager.getOrStub(1L) } returns dex
        every { harness.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        harness.start()
    }

    @After
    fun tearDown() {
        mangaDexSourceIds = previousMangaDex
        harness.stop()
    }

    @Test
    fun randomOpensItsManga() {
        compose.setContent { ScreenHost(SourceFeedScreen(1L)) }
        compose.pollLabel("Dex manga")
        // Without filters the button only offers the saved searches; it opens the same sheet.
        compose.clickLabel("Saved Searches")
        compose.pollLabel("Random")
        compose.clickLabel("Random")
        compose.pollLabel("opened:BrowseSourceScreen")
    }
}
