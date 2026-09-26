package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import exh.favorites.FavoritesSyncStatus
import exh.recs.batch.SearchStatus
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabProgressTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun syncStatus(status: FavoritesSyncStatus) {
        rig.model().favoritesSync.status.value = status
    }

    private fun searchStatus(status: SearchStatus) {
        rig.model().recommendationSearch.status.value = status
    }

    @Test
    fun syncErrorOpensTheGallery() {
        rig.show()
        val model = rig.model()
        syncStatus(FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories(1, "Alpha", listOf("A", "B")))
        rig.waitFor("Show Gallery")
        rig.click("Show Gallery")
        rig.waitFor("opened:MangaScreen")
        model.favoritesSync.status.value shouldBe FavoritesSyncStatus.Idle
    }

    @Test
    fun syncErrorsAreDismissed() {
        rig.show()
        syncStatus(FavoritesSyncStatus.CompleteWithErrors(emptyList()))
        rig.click("OK")
        compose.waitUntil(LIBRARY_WAIT) { rig.model().favoritesSync.status.value == FavoritesSyncStatus.Idle }
    }

    @Test
    fun searchIsCancelled() {
        rig.show()
        searchStatus(SearchStatus.Initializing)
        rig.click("Cancel")
        compose.waitUntil(LIBRARY_WAIT) { rig.model().recommendationSearch.status.value == SearchStatus.Idle }
    }

    @Test
    fun searchErrorsAreDismissed() {
        rig.show()
        searchStatus(SearchStatus.Error("boom"))
        rig.click("OK")
        compose.waitUntil(LIBRARY_WAIT) { rig.model().recommendationSearch.status.value == SearchStatus.Idle }
    }

    @Test
    fun searchWithoutResults() {
        rig.show()
        searchStatus(SearchStatus.Finished.WithoutResults)
        compose.waitUntil(LIBRARY_WAIT) { rig.model().recommendationSearch.status.value == SearchStatus.Idle }
        ShadowToast.getTextOfLatestToast() shouldBe "No recommendations found"
    }

    @Test
    fun searchResultsOpen() {
        rig.show()
        searchStatus(SearchStatus.Finished.WithResults(emptyList()))
        rig.waitFor("opened:RecommendsScreen")
    }
}
