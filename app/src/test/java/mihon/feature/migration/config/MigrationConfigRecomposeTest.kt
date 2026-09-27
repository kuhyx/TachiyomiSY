package mihon.feature.migration.config

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.presentation.util.Churn
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Recomposes the migration config pieces unchanged, then with a new screen model. */
@RunWith(RobolectricTestRunner::class)
internal class MigrationConfigRecomposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = MigrationConfigHarness()
    private val churn = Churn(compose)

    @Before
    fun setUp() {
        harness.start()
        harness.sources = {
            listOf(httpSource(1L, name = "Alpha"), httpSource(2L, name = "Beta", lang = "ja"), httpSource(3L))
        }
        harness.preferences.migrationSources.set(listOf(1L, 2L))
    }

    @After
    fun tearDown() {
        clearVoyagerScopes()
        harness.stop()
    }

    @Test
    fun listsRecompose() {
        val first = harness.model().apply { awaitLoaded() }
        val second = harness.model().apply { awaitLoaded() }
        var model by mutableStateOf(first)
        compose.setContent {
            churn.Host {
                MaterialTheme {
                    val state by model.state.collectAsState()
                    val (selected, available) = state.sources.partition { it.isSelected }
                    Column {
                        SelectionActions(model)
                        SourceLists(model, selected, available, true, rememberLazyListState(), PaddingValues())
                    }
                }
            }
        }
        compose.waitForLabel("Alpha")
        churn.rerun()
        model = second
        compose.waitForIdle()
        churn.rerun()
    }

    @Test
    fun screenRecomposes() {
        compose.setContent { churn.Host { ScreenHost(MigrationConfigScreen(listOf(1L, 2L))) } }
        compose.waitForLabel("Alpha")
        churn.rerun()
    }
}
