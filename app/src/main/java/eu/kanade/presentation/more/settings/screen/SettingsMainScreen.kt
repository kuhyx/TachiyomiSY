package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

// The icon tint nudges the HSV value slightly off the surface colour.
private const val HSV_COMPONENTS = 3
private const val DARK_VALUE_SHIFT = 0.05f
private const val LIGHT_VALUE_SHIFT = 0.02f

internal object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(HSV_COMPONENTS)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - DARK_VALUE_SHIFT
            } else {
                arr[2] + LIGHT_VALUE_SHIFT
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding -> SettingsList(twoPane, topBarState, contentPadding) },
        )
    }

    // In two-pane mode the entry whose screen is open on the right is highlighted and scrolled into view.
    @Composable
    private fun SettingsList(twoPane: Boolean, topBarState: TopAppBarState, contentPadding: PaddingValues) {
        val navigator = LocalNavigator.currentOrThrow
        val state = rememberLazyListState()
        // SY -->
        val items = settingsMainItems.filter { it.screen !is SearchableSettings || it.screen.isEnabled() }
        // SY <--
        val indexSelected = if (twoPane) {
            items.indexOfFirst { it.screen::class == navigator.items.first()::class }
                .also {
                    LaunchedEffect(Unit) {
                        state.animateScrollToItem(it)
                        if (it > 0) {
                            // Lift scroll
                            topBarState.contentOffset = topBarState.heightOffsetLimit
                        }
                    }
                }
        } else {
            null
        }

        LazyColumn(
            state = state,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.hashCode() },
            ) { index, item ->
                SettingsEntry(item, twoPane = twoPane, selected = indexSelected == index) {
                    navigator.navigate(item.screen, twoPane)
                }
            }
        }
    }

    @Composable
    private fun SettingsEntry(item: SettingsMainItem, twoPane: Boolean, selected: Boolean, onClick: () -> Unit) {
        var modifier: Modifier = Modifier
        var contentColor = LocalContentColor.current
        if (twoPane) {
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(
                    if (selected) {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    } else {
                        Modifier
                    },
                )
            if (selected) {
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            TextPreferenceWidget(
                modifier = modifier,
                title = stringResource(item.titleRes),
                subtitle = item.formatSubtitle(),
                icon = item.icon,
                onPreferenceClick = onClick,
            )
        }
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }
}
