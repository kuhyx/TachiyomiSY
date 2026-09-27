package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import exh.source.mangaDexSourceIds
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A MangaDex source's random pick, and a top bar whose model cannot filter. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceMangaDexTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)
    private val previousMangaDex = mangaDexSourceIds

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        mangaDexSourceIds = previousMangaDex
        rig.stop()
    }

    @Test
    fun randomOpensItsManga() {
        mangaDexSourceIds = listOf(1L)
        val dex = mockk<MangaDex>(relaxed = true) {
            every { id } returns 1L
            every { name } returns "Dex"
            every { getFilterList() } returns FilterList()
            coEvery { fetchRandomMangaUrl() } returns "/random"
        }
        every { rig.harness.sourceManager.getOrStub(1L) } returns dex
        // A metadata source's rows wait on metadata this rig never serves; list nothing instead.
        rig.items = emptyList()
        rig.show()
        rig.await("Search")
        rig.click("Search")
        rig.await("Random")
        rig.click("Random")
        rig.await("opened:BrowseSourceScreen")
    }

    @Test
    fun unfilterableHidesSearchChip() {
        val model = rig.harness.model()
        val unfilterable = model.state.value.copy(filterable = false)
        compose.setContent {
            MaterialTheme {
                Navigator(BlankScreen()) { navigator ->
                    BrowseSourceTopBar(
                        screenModel = model,
                        state = unfilterable,
                        navigator = navigator,
                        navigateUp = {},
                        onWebViewClick = {},
                        onHelpClick = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.hasLabel("Popular") shouldBe true
        // The toolbar's search icon is described as "Search"; the chip would be text.
        compose.onAllNodes(hasText("Search")).fetchSemanticsNodes().size shouldBe 0
        // A real touch passes through the bar, which swallows pointer input meant for the list below it.
        compose.onNodeWithText("Popular").performClick()
        compose.waitForIdle()
    }
}
