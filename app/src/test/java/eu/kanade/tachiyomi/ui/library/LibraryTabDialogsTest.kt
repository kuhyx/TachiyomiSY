package eu.kanade.tachiyomi.ui.library

import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.libraryManga
import exh.favorites.FavoritesSyncStatus
import exh.recs.batch.SearchStatus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)

    @Before
    fun setUp() {
        rig.start()
        coEvery { rig.harness.getCategories.await(1L) } returns listOf(rig.reading)
    }

    @After
    fun tearDown() = rig.stop()

    private fun selectAlpha() {
        rig.show()
        rig.select("Alpha")
        rig.waitFor("Mark as read")
    }

    @Test
    fun reselectOpensTheSettings() {
        rig.harness.categories.value = listOf(Category(id = 0, name = "", order = 0, flags = 0), rig.reading)
        rig.show()
        CoroutineScope(Dispatchers.IO).launch { LibraryTab.onReselect(mockk<Navigator>()) }
        rig.waitFor("Sort")
        rig.model().state.value.dialog shouldBe LibraryScreenModel.Dialog.SettingsSheet
    }

    @Test
    fun changingCategories() {
        selectAlpha()
        rig.click("Set categories")
        rig.waitFor("Edit")
        rig.click("OK")
        coVerify(timeout = LIBRARY_WAIT) { rig.harness.setMangaCategories.await(1L, any()) }
        rig.select("Alpha")
        rig.click("Set categories")
        rig.waitFor("Edit")
        rig.click("Edit")
        rig.waitFor("opened:CategoryScreen")
    }

    @Test
    fun deletingEntries() {
        selectAlpha()
        rig.click("Delete")
        rig.click("From library")
        rig.click("OK")
        coVerify(timeout = LIBRARY_WAIT) { rig.harness.updateManga.awaitAll(any()) }
        rig.waitUntilGone("Mark as read")
    }

    @Test
    fun favoritesSyncWarnsThenConfirms() {
        rig.harness.exhPreferences.isHentaiEnabled.set(true)
        rig.harness.exhPreferences.exhShowSyncIntro.set(true)
        rig.show()
        rig.overflow("Sync EH favorites")
        rig.waitFor("IMPORTANT FAVORITES SYNC NOTES")
        rig.click("OK")
        rig.harness.exhPreferences.exhShowSyncIntro.get() shouldBe false
        val model = rig.model()
        rig.overflow("Sync EH favorites")
        rig.click("OK")
        // Not logged in to ExHentai: the sync stops at once with an error.
        compose.waitUntil(LIBRARY_WAIT) {
            model.favoritesSync.status.value == FavoritesSyncStatus.SyncError.NotLoggedInSyncError
        }
    }

    @Test
    fun recommendationSearchStarts() {
        coEvery { rig.harness.getLibraryManga.await() } coAnswers { awaitCancellation() }
        rig.harness.library.value = listOf(
            libraryManga(1, manga(1, "Alpha"), categories = listOf(1L)),
            libraryManga(2, manga(2, "Beta"), categories = listOf(1L)),
        )
        // The sheet inflates a themed XML layout.
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        selectAlpha()
        rig.select("Beta")
        rig.click("More")
        rig.click("Find recommendations")
        compose.waitUntil(LIBRARY_WAIT) { ShadowDialog.getLatestDialog() != null }
        ShadowDialog.getLatestDialog().findViewById<Button>(R.id.rec_search_btn).performClick()
        compose.waitUntil(LIBRARY_WAIT) { rig.model().recommendationSearch.status.value == SearchStatus.Initializing }
        rig.model().cancelRecommendationSearch()
    }
}
