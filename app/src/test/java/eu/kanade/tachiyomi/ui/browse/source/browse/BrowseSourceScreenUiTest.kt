package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.model.StubSource

/** The whole browse screen: loading, the listing chips, the result grid and a long press on an entry. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun waitsForSources() {
        rig.loaded.value = false
        rig.show()
        compose.hasLabel("Popular") shouldBe false
        rig.loaded.value = true
        rig.await("Popular")
        rig.await("Manga 1")
    }

    @Test
    fun missingSourceScreen() {
        every { rig.harness.sourceManager.getOrStub(1L) } returns StubSource(id = 1L, lang = "en", name = "Gone")
        rig.show()
        rig.await("Source not installed: Gone (EN)")
    }

    @Test
    fun chipsSwitchListing() {
        every { rig.harness.source.supportsLatest } returns true
        rig.show()
        rig.click("Latest")
        rig.click("Popular")
        rig.await("Search")
        rig.click("Search")
        rig.await("Reset")
    }

    @Test
    fun filterChipNamesFilters() {
        rig.harness.filters = { FilterList(object : Filter.CheckBox("Check") {}) }
        every { rig.harness.source.supportsLatest } returns false
        rig.show()
        rig.await("Filter")
        compose.hasLabel("Latest") shouldBe false
    }

    @Test
    fun emptyListingSaysSo() {
        rig.items = emptyList()
        rig.show()
        rig.await("No results found")
    }

    @Test
    fun clickOpensManga() {
        rig.show()
        rig.await("Manga 1")
        rig.click("Manga 1")
        rig.await("opened:MangaScreen")
    }

    @Test
    fun longPressAddsFavorite() {
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        coVerify(timeout = 5_000) { rig.harness.updateManga.await(match { it.id == 1L && it.favorite == true }) }
    }

    @Test
    fun longPressOffersRemoval() {
        rig.items = listOf(listed(1L, favorite = true))
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Remove")
        rig.click("Remove")
        coVerify(timeout = 5_000) { rig.harness.updateManga.await(match { it.id == 1L && it.favorite == false }) }
    }

    @Test
    fun longPressWarnsOfDuplicates() {
        coEvery { rig.harness.getDuplicates(any()) } returns listOf(MangaWithChapterCount(listed(9L), 1))
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Possible duplicates")
        rig.click("Add anyway")
        coVerify(timeout = 5_000) { rig.harness.updateManga.await(match { it.id == 1L && it.favorite == true }) }
    }

    @Test
    fun outsideSearchesLand() {
        rig.harness.filters = { FilterList(object : Filter.CheckBox("Action") {}) }
        rig.show()
        rig.await("Manga 1")
        // The query channel is global: an unreceived send would reach a later class's screen, so it is cancelled.
        val sender = CoroutineScope(Dispatchers.Default)
        try {
            sender.launch {
                BrowseSourceScreen(1L, "").searchGenre("Action")
                BrowseSourceScreen(1L, "").search("typed")
            }
            rig.await("typed")
        } finally {
            sender.cancel()
        }
    }
}
