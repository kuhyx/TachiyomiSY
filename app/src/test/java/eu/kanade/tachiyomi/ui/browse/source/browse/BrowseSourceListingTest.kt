package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.library.hasLabel
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A listing without a query, hiding what the library already holds, and what counts as a user query. */
@RunWith(RobolectricTestRunner::class)
internal class BrowseSourceListingTest {
    @get:Rule
    val compose = createComposeRule()

    private val rig = BrowseScreenRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun missingQuerySearchesBlank() {
        rig.show(BrowseSourceScreen(1L, null))
        rig.await("Manga 1")
        verify { rig.harness.getRemoteManga(1L, "", any()) }
    }

    @Test
    fun libraryItemsCanBeHidden() {
        rig.harness.koin.sourcePreferences.hideInLibraryItems.set(true)
        rig.items = listOf(listed(1L, favorite = true), listed(2L))
        rig.show()
        rig.await("Manga 2")
        compose.hasLabel("Manga 1") shouldBe false
    }

    @Test
    fun typedSearchesAreUserQueries() {
        fun query(listing: Listing) = BrowseSourceScreenModel.State(listing).isUserQuery
        query(Listing.Popular) shouldBe false
        query(Listing.Search(query = null, filters = FilterList())) shouldBe false
        query(Listing.Search(query = "", filters = FilterList())) shouldBe false
        query(Listing.Search(query = "typed", filters = FilterList())) shouldBe true
    }
}
