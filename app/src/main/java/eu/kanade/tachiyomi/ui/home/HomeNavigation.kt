package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
internal fun HomeNavigationRail(alwaysShowLabel: Boolean) {
    NavigationRail {
        HomeScreen.TABS
            // SY -->
            .fastFilter { it.isEnabled() }
            // SY <--
            .fastForEach {
                NavigationRailItem(it/* SY --> */, alwaysShowLabel/* SY <-- */)
            }
    }
}

@Composable
internal fun HomeNavigationBar(alwaysShowLabel: Boolean) {
    val bottomNavVisible by produceState(initialValue = true) {
        HomeScreen.showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
    }
    AnimatedVisibility(
        visible = bottomNavVisible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        NavigationBar {
            HomeScreen.TABS
                // SY -->
                .fastFilter { it.isEnabled() }
                // SY <--
                .fastForEach {
                    NavigationBarItem(it/* SY --> */, alwaysShowLabel/* SY <-- */)
                }
        }
    }
}

@Composable
internal fun RowScope.NavigationBarItem(
    tab: eu.kanade.presentation.util.Tab, /* SY --> */
    alwaysShowLabel: Boolean, /* SY <-- */
) {
    val tabNavigator = LocalTabNavigator.current
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    val selected = tabNavigator.current::class == tab::class
    NavigationBarItem(
        selected = selected,
        onClick = {
            if (!selected) {
                tabNavigator.current = tab
            } else {
                scope.launch { tab.onReselect(navigator) }
            }
        },
        icon = { NavigationIconItem(tab) },
        label = {
            Text(
                text = tab.options.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        alwaysShowLabel = /* SY --> */alwaysShowLabel, /* SY <-- */
    )
}

@Composable
internal fun NavigationRailItem(
    tab: eu.kanade.presentation.util.Tab/* SY --> */,
    alwaysShowLabel: Boolean/* SY <-- */,
) {
    val tabNavigator = LocalTabNavigator.current
    val navigator = LocalNavigator.currentOrThrow
    val scope = rememberCoroutineScope()
    val selected = tabNavigator.current::class == tab::class
    NavigationRailItem(
        selected = selected,
        onClick = {
            if (!selected) {
                tabNavigator.current = tab
            } else {
                scope.launch { tab.onReselect(navigator) }
            }
        },
        icon = { NavigationIconItem(tab) },
        label = {
            Text(
                text = tab.options.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        alwaysShowLabel = /* SY --> */alwaysShowLabel, /* SY <-- */
    )
}

@Composable
internal fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
    BadgedBox(
        badge = {
            when {
                tab is UpdatesTab -> {
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<LibraryPreferences>()
                        combine(
                            pref.newShowUpdatesCount.changes(),
                            pref.newUpdatesCount.changes(),
                        ) { show, count -> if (show) count else 0 }
                            .collectLatest { value = it }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                MR.plurals.notification_chapters_generic,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }

                BrowseTab::class.isInstance(tab) -> {
                    val count by produceState(initialValue = 0) {
                        Injekt.get<SourcePreferences>().extensionUpdatesCount.changes()
                            .collectLatest { value = it }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                MR.plurals.update_check_notification_ext_updates,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Icon(
            painter = tab.options.icon!!,
            contentDescription = tab.options.title,
        )
    }
}
