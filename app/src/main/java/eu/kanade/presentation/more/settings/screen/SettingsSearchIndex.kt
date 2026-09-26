package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.presentation.core.i18n.stringResource
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

private const val MAX_RESULTS = 10

// Pure search over the settings index; only the top MAX_RESULTS hits are kept for a quicker answer.
internal fun searchIndex(index: List<SettingsData>, searchKey: String, isLtr: Boolean): List<SearchResultItem> {
    return index.asSequence()
        .flatMap { settingsData ->
            searchableEntries(settingsData.contents)
                // Filter by search query
                .filter { (_, p) ->
                    val inTitle = p.title.contains(searchKey, true)
                    val inSummary = p.subtitle?.contains(searchKey, true) ?: false
                    inTitle || inSummary
                }
                .map { (categoryTitle, p) ->
                    SearchResultItem(
                        route = settingsData.route,
                        title = p.title,
                        breadcrumbs = getLocalizedBreadcrumb(
                            path = settingsData.title,
                            node = categoryTitle,
                            isLtr = isLtr,
                        ),
                        highlightKey = p.title,
                    )
                }
        }
        .take(MAX_RESULTS)
        .toList()
}

// (group title or null) -> item, for every enabled, titled, non-info preference on a screen.
internal fun searchableEntries(contents: List<Preference>): Sequence<Pair<String?, Preference.PreferenceItem<*, *>>> {
    return contents.asSequence()
        // Only search from enabled prefs and one with valid title
        .filter { it.enabled && it.title.isNotBlank() }
        // Flatten items contained inside *enabled* PreferenceGroup
        .flatMap { p ->
            when (p) {
                // The filter above already dropped disabled groups.
                is Preference.PreferenceGroup -> {
                    p.preferenceItems.asSequence()
                        .filter { it.enabled && it.title.isNotBlank() }
                        .map { p.title to it }
                }
                is Preference.PreferenceItem<*, *> -> {
                    sequenceOf(null to p)
                }
            }
        }
        // Don't show info preference
        .filterNot { it.second is Preference.PreferenceItem.InfoPreference }
}

@Composable
@NonRestartableComposable
internal fun getIndex() = settingScreens
    // SY -->
    .filter(SearchableSettings::isEnabled)
    // SY <--
    .map { screen ->
        SettingsData(
            title = stringResource(screen.getTitleRes()),
            route = screen,
            contents = screen.getPreferences(),
        )
    }

internal fun getLocalizedBreadcrumb(path: String, node: String?, isLtr: Boolean): String {
    return if (node == null) {
        path
    } else {
        if (isLtr) {
            // This locale reads left to right.
            "$path > $node"
        } else {
            // This locale reads right to left.
            "$node < $path"
        }
    }
}

private val settingScreens = listOf(
    SettingsAppearanceScreen,
    SettingsLibraryScreen,
    SettingsReaderScreen,
    SettingsDownloadScreen,
    SettingsTrackingScreen,
    SettingsBrowseScreen,
    SettingsDataScreen,
    SettingsSecurityScreen,
    // SY -->
    SettingsEhScreen,
    SettingsMangadexScreen,
    // SY <--
    SettingsAdvancedScreen,
)

internal data class SettingsData(
    val title: String,
    val route: VoyagerScreen,
    val contents: List<Preference>,
)

internal data class SearchResultItem(
    val route: VoyagerScreen,
    val title: String,
    val breadcrumbs: String,
    val highlightKey: String,
)
