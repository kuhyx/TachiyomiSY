package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.Dialog
import exh.source.EH_SOURCE_ID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Recomposes each section of the tab through a host composable: unchanged, with a new screen model, then
 * with a new state.
 */
@RunWith(RobolectricTestRunner::class)
internal class LibraryTabRecomposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = LibraryHarness()
    private var tick by mutableIntStateOf(0)
    private lateinit var first: LibraryScreenModel
    private lateinit var second: LibraryScreenModel
    private lateinit var model: MutableState<LibraryScreenModel>
    private var state by mutableStateOf(LibraryScreenModel.State())

    @Before
    fun setUp() {
        harness.start()
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        first = harness.model()
        second = harness.model()
        model = mutableStateOf(first)
    }

    @After
    fun tearDown() = harness.stop()

    private fun churn(next: LibraryScreenModel.State) {
        tick++
        compose.waitForIdle()
        model.value = if (model.value === first) second else first
        compose.waitForIdle()
        state = next
        compose.waitForIdle()
    }

    private fun entries(): LibraryScreenModel.State {
        val gallery = libItem(libEntry(libManga(1L, "[G] A", source = EH_SOURCE_ID)))
        val items = listOf(gallery, libItem(2L))
        return stateOf(libCategory(1L), items, selection = setOf(1L, 2L)).copy(isLoading = false)
    }

    private fun host(content: @Composable () -> Unit) {
        compose.setContent {
            ScreenHost(
                ComposableScreen {
                    Text("r$tick")
                    content()
                },
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun barsRecompose() {
        state = entries()
        host { BarsHost(model.value, state) }
        churn(entries().copy(selection = emptySet(), showCategoryTabs = true))
        churn(entries())
    }

    @Test
    fun bodyRecomposes() {
        state = entries().copy(selection = emptySet(), showMangaContinueButton = true)
        host { BodyHost(model.value, state) }
        churn(state.copy(searchQuery = "a"))
        churn(state.copy(searchQuery = null))
    }

    @Test
    fun dialogsRecompose() {
        val settings = LibrarySettingsScreenModel()
        val dialogs = listOf(
            Dialog.SettingsSheet,
            Dialog.ChangeCategory(emptyList(), emptyList()),
            Dialog.DeleteManga(listOf(libManga(1L))),
            Dialog.SyncFavoritesWarning,
            Dialog.SyncFavoritesConfirm,
            null,
        )
        state = LibraryScreenModel.State(dialog = Dialog.SyncFavoritesConfirm)
        host { DialogsHost(model.value, settings, state) }
        dialogs.forEach { churn(state.copy(dialog = it)) }
    }
}

@Composable
private fun BarsHost(model: LibraryScreenModel, state: LibraryScreenModel.State) {
    val behavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    Column {
        LibraryTabToolbar(
            screenModel = model,
            state = state,
            snackbarHostState = snackbar,
            onClickRefresh = { false },
            scrollBehavior = behavior,
        )
        LibraryTabBottomBar(model, state)
    }
}

@Composable
private fun BodyHost(model: LibraryScreenModel, state: LibraryScreenModel.State) {
    val snackbar = remember { SnackbarHostState() }
    LibraryTabBody(
        screenModel = model,
        state = state,
        contentPadding = PaddingValues(),
        snackbarHostState = snackbar,
        onClickRefresh = { false },
    )
}

@Composable
private fun DialogsHost(
    model: LibraryScreenModel,
    settings: LibrarySettingsScreenModel,
    state: LibraryScreenModel.State,
) {
    LibraryTabDialogs(model, settings, state)
    val dialog = state.dialog
    if (dialog != null) SyDialogs(model, dialog) {}
    RecommendationResultEffect(model)
}
