package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import exh.source.mangaDexSourceIds
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.source.model.EXHSavedSearch

/** The filter sheet, the saved-search dialogs, the category picker and the migrate dialog. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)
    private val previousMangaDex = mangaDexSourceIds
    private val kept = EXHSavedSearch(id = 4L, name = "Kept", query = "q", filterList = null)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() {
        mangaDexSourceIds = previousMangaDex
        rig.stop()
    }

    private fun openFilters() {
        rig.show()
        rig.await("Search")
        rig.click("Search")
        rig.await("Reset")
    }

    @Test
    fun filterSheetFilters() {
        rig.harness.filters = { FilterList(object : Filter.CheckBox("Check") {}) }
        rig.show()
        rig.click("Filter")
        rig.click("Check")
        rig.click("Reset")
        rig.click("Filter")
        rig.await("Manga 1")
    }

    @Test
    fun savingASearch() {
        openFilters()
        rig.click("Save")
        rig.await("OK")
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Mine")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.insertSavedSearch.await(match { it.name == "Mine" }) }
    }

    @Test
    fun savedSearchAppliesAndDeletes() {
        rig.harness.savedSearches.value = listOf(kept)
        openFilters()
        rig.click("Kept")
        rig.await("q")
        rig.click("Search")
        rig.await("Kept")
        compose.onNodeWithText("Kept").performTouchInput { longClick() }
        rig.await("OK")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.deleteSavedSearch.await(4L) }
    }

    @Test
    fun invalidSavedSearchToasts() {
        rig.harness.filters = { FilterList(object : Filter.CheckBox("Check") {}) }
        rig.harness.savedSearches.value = listOf(kept)
        rig.show()
        rig.click("Filter")
        rig.click("Kept")
        rig.await("Reset")
        compose.waitUntil(timeoutMillis = 10_000) {
            ShadowLooper.idleMainLooper()
            ShadowToast.getTextOfLatestToast() == "Saved search invalid, filters have changed"
        }
    }

    @Test
    fun mangaDexFollowsReplace() {
        mangaDexSourceIds = listOf(1L)
        openFilters()
        rig.click("Random")
        rig.click("MangaDex follows")
        rig.await("opened:MangaDexFollowsScreen")
    }

    @Test
    fun categoryPickerConfirms() {
        val category = Category(id = 2L, name = "Two", order = 1L, flags = 0L)
        rig.harness.categories.value = listOf(category)
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Two")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.setMangaCategories.await(1L, any()) }
    }

    @Test
    fun categoryPickerEdits() {
        val category = Category(id = 2L, name = "Two", order = 1L, flags = 0L)
        rig.harness.categories.value = listOf(category)
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Edit")
        rig.click("Edit")
        rig.await("opened:CategoryScreen")
    }

    @Test
    fun duplicateOpensOrMigrates() {
        coEvery { rig.harness.getDuplicates(any()) } returns listOf(MangaWithChapterCount(listed(9L), 1))
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Manga 9")
        rig.click("Manga 9")
        rig.await("Migrate")
        rig.click("Manga 1")
        rig.await("opened:MangaScreen")
    }

    @Test
    fun duplicateLongPressOpens() {
        coEvery { rig.harness.getDuplicates(any()) } returns listOf(MangaWithChapterCount(listed(9L), 1))
        rig.show()
        rig.await("Manga 1")
        compose.onNodeWithText("Manga 1").performTouchInput { longClick() }
        rig.await("Manga 9")
        compose.onNodeWithText("Manga 9").performTouchInput { longClick() }
        rig.await("opened:MangaScreen")
    }
}
