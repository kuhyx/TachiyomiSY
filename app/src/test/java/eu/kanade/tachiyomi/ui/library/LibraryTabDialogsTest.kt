package eu.kanade.tachiyomi.ui.library

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.Dialog
import eu.kanade.tachiyomi.ui.manga.eventually
import exh.favorites.FavoritesSyncStatus
import exh.recs.batch.SearchStatus
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabDialogsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = LibraryHarness()
    private var state by mutableStateOf(LibraryScreenModel.State())
    private lateinit var model: LibraryScreenModel

    @Before
    fun setUp() {
        harness.start()
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
    }

    @After
    fun tearDown() = harness.stop()

    private fun show(dialog: Dialog?) {
        model = harness.model()
        val settings = LibrarySettingsScreenModel()
        state = LibraryScreenModel.State(dialog = dialog)
        compose.setContent { ScreenHost(ComposableScreen { LibraryTabDialogs(model, settings, state) }) }
        compose.waitForIdle()
    }

    private fun click(label: String) {
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun settingsSheetFollowsTheState() {
        show(null)
        compose.hasLabel("Filter") shouldBe false
        state = state.copy(dialog = Dialog.SettingsSheet)
        compose.waitForLabel("Filter")
        val categories = listOf(libCategory(0L), libCategory(1L))
        state = state.copy(libraryData = LibraryScreenModel.LibraryData(categories = categories))
        compose.waitForIdle()
        compose.hasLabel("Filter") shouldBe true
    }

    @Test
    fun editCategoriesOpensTheScreen() {
        show(Dialog.ChangeCategory(listOf(libManga(1L)), listOf(CheckboxState.State.Checked(libCategory(1L)))))
        click("Edit")
        compose.waitForLabel("opened:CategoryScreen")
    }

    @Test
    fun confirmingCategoriesMoves() {
        show(Dialog.ChangeCategory(listOf(libManga(1L)), listOf(CheckboxState.State.Checked(libCategory(1L)))))
        click("OK")
        coVerify(timeout = WAIT) { harness.setMangaCategories.await(1L, listOf(1L)) }
    }

    @Test
    fun deletingLocalEntries() {
        show(Dialog.DeleteManga(listOf(libManga(1L, source = LocalSource.ID))))
        click("From library")
        click("OK")
        coVerify(timeout = WAIT) { harness.updateManga.awaitAll(any()) }
    }

    @Test
    fun deletingDownloads() {
        show(Dialog.DeleteManga(listOf(libManga(1L))))
        click("Downloaded chapters")
        click("OK")
        verify(timeout = WAIT) { harness.sourceManager.get(1L) }
    }

    @Test
    fun syncWarningIsAccepted() {
        show(Dialog.SyncFavoritesWarning)
        click("OK")
        harness.exhPreferences.exhShowSyncIntro.get() shouldBe false
    }

    @Test
    fun syncConfirmStartsTheSync() {
        show(Dialog.SyncFavoritesConfirm)
        click("OK")
        eventually { model.favoritesSync.status.value == FavoritesSyncStatus.SyncError.NotLoggedInSyncError }
    }

    @Test
    fun recSheetStartsTheSearch() {
        show(Dialog.RecommendationSearchSheet(emptyList()))
        val button = ShadowDialog.getLatestDialog().findViewById<View>(R.id.rec_search_btn)
        button.performClick()
        compose.waitForIdle()
        eventually { model.recommendationSearch.status.value == SearchStatus.Finished.WithoutResults }
    }

    @Test
    fun syDialogsIgnoreOtherDialogs() {
        val model = harness.model()
        compose.setContent { SyDialogs(model, Dialog.SettingsSheet) {} }
        compose.waitForIdle()
        compose.hasLabel("OK") shouldBe false
    }
}
