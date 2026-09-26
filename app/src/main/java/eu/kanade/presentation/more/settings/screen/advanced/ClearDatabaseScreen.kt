package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LazyColumnWithAction
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.api.get

internal class ClearDatabaseScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { ClearDatabaseScreenModel() }
        val state by model.state.collectAsState()

        // Loading is the `else`: a sealed `when` would get a dead "no match" group from Compose.
        val s = state
        if (s is ClearDatabaseScreenModel.State.Ready) {
            if (s.showConfirmation) {
                ConfirmClearDialog(model)
            }

            Scaffold(
                topBar = { scrollBehavior ->
                    AppBar(
                        title = stringResource(MR.strings.pref_clear_database),
                        navigateUp = navigator::pop,
                        actions = { if (s.items.isNotEmpty()) SelectionActions(model) },
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) { contentPadding ->
                if (s.items.isEmpty()) {
                    EmptyScreen(
                        message = stringResource(MR.strings.database_clean),
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    LazyColumnWithAction(
                        contentPadding = contentPadding,
                        actionLabel = stringResource(MR.strings.action_delete),
                        actionEnabled = s.selection.isNotEmpty(),
                        onClickAction = model::showConfirmation,
                    ) {
                        items(s.items) { sourceWithCount ->
                            ClearDatabaseItem(
                                source = sourceWithCount.source,
                                count = sourceWithCount.count,
                                isSelected = s.selection.contains(sourceWithCount.id),
                                onClickSelect = { model.toggleSelection(sourceWithCount.source) },
                            )
                        }
                    }
                }
            }
        } else {
            LoadingScreen()
        }
    }
}
