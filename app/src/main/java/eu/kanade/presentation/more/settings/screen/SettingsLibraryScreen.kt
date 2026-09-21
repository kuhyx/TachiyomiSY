package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TriStateListDialog
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.setupTask
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreen
import kotlinx.coroutines.launch
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_EXISTING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MARK_DUPLICATE_CHAPTER_READ_NEW
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Library update interval choices, in hours.
private const val TWELVE_HOURS = 12
private const val ONE_DAY_HOURS = 24
private const val TWO_DAYS_HOURS = 48
private const val THREE_DAYS_HOURS = 72
private const val ONE_WEEK_HOURS = 168

internal object SettingsLibraryScreen : SearchableSettings {

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.pref_category_library

    @Composable
    override fun getPreferences(): List<Preference> {
        val getCategories = remember { Injekt.get<GetCategories>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val allCategories by getCategories.subscribe().collectAsState(initial = emptyList())

        return listOf(
            getCategoriesGroup(LocalNavigator.currentOrThrow, allCategories, libraryPreferences),
            getGlobalUpdateGroup(allCategories, libraryPreferences),
            getBehaviorGroup(libraryPreferences),
            // SY -->
            getSortingCategory(LocalNavigator.currentOrThrow, libraryPreferences),
            // SY <--
        )
    }

    @Composable
    private fun getCategoriesGroup(
        navigator: Navigator,
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val scope = rememberCoroutineScope()
        val userCategoriesCount = allCategories.filterNot(Category::isSystemCategory).size

        // For default category
        val ids = listOf(libraryPreferences.defaultCategory.defaultValue()) +
            allCategories.fastMap { it.id.toInt() }
        val labels = listOf(stringResource(MR.strings.default_category_summary)) +
            allCategories.fastMap { it.visualName }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.categories),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.action_edit_categories),
                    subtitle = pluralStringResource(
                        MR.plurals.num_categories,
                        count = userCategoriesCount,
                        userCategoriesCount,
                    ),
                    onClick = { navigator.push(CategoryScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.defaultCategory,
                    entries = ids.zip(labels).toMap(),
                    title = stringResource(MR.strings.default_category),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = libraryPreferences.categorizedDisplaySettings,
                    title = stringResource(MR.strings.categorized_display_settings),
                    onValueChanged = {
                        if (!it) {
                            scope.launch {
                                Injekt.get<ResetCategoryFlags>().await()
                            }
                        }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getGlobalUpdateGroup(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val autoUpdateInterval by libraryPreferences.autoUpdateInterval.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_library_update),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.autoUpdateInterval,
                    entries = mapOf(
                        0 to stringResource(MR.strings.update_never),
                        TWELVE_HOURS to stringResource(MR.strings.update_12hour),
                        ONE_DAY_HOURS to stringResource(MR.strings.update_24hour),
                        TWO_DAYS_HOURS to stringResource(MR.strings.update_48hour),
                        THREE_DAYS_HOURS to stringResource(MR.strings.update_72hour),
                        ONE_WEEK_HOURS to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_library_update_interval),
                    onValueChanged = {
                        LibraryUpdateJob.setupTask(context, it)
                        true
                    },
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.autoUpdateDeviceRestrictions,
                    entries = mapOf(
                        DEVICE_ONLY_ON_WIFI to stringResource(MR.strings.connected_to_wifi),
                        DEVICE_NETWORK_NOT_METERED to stringResource(MR.strings.network_not_metered),
                        DEVICE_CHARGING to stringResource(MR.strings.charging),
                    ),
                    title = stringResource(MR.strings.pref_library_update_restriction),
                    subtitle = stringResource(MR.strings.restrictions),
                    enabled = autoUpdateInterval > 0,
                    onValueChanged = {
                        // Post to event looper to allow the preference to be updated.
                        ContextCompat.getMainExecutor(context).execute { LibraryUpdateJob.setupTask(context) }
                        true
                    },
                ),
                updateCategoriesPreference(allCategories, libraryPreferences),
            ) + updateScopePreferences(libraryPreferences),
        )
    }

    // Which categories a global update covers; the tri-state dialog writes the include/exclude sets.
    @Composable
    private fun updateCategoriesPreference(
        allCategories: List<Category>,
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceItem<out Any, out Any> {
        val autoUpdateCategoriesPref = libraryPreferences.updateCategories
        val autoUpdateCategoriesExcludePref = libraryPreferences.updateCategoriesExclude
        val included by autoUpdateCategoriesPref.collectAsState()
        val excluded by autoUpdateCategoriesExcludePref.collectAsState()
        var showCategoriesDialog by rememberSaveable { mutableStateOf(false) }
        if (showCategoriesDialog) {
            TriStateListDialog(
                title = stringResource(MR.strings.categories),
                message = stringResource(MR.strings.pref_library_update_categories_details),
                items = allCategories,
                initialChecked = included.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                initialInversed = excluded.mapNotNull { id -> allCategories.find { it.id.toString() == id } },
                itemLabel = { it.visualName },
                onDismissRequest = { showCategoriesDialog = false },
                onValueChanged = { newIncluded, newExcluded ->
                    autoUpdateCategoriesPref.set(newIncluded.map { it.id.toString() }.toSet())
                    autoUpdateCategoriesExcludePref.set(newExcluded.map { it.id.toString() }.toSet())
                    showCategoriesDialog = false
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.categories),
            subtitle = getCategoriesLabel(
                allCategories = allCategories,
                included = included,
                excluded = excluded,
            ),
            onClick = { showCategoriesDialog = true },
        )
    }

    @Composable
    private fun updateScopePreferences(
        libraryPreferences: LibraryPreferences,
    ): List<Preference.PreferenceItem<out Any, out Any>> {
        return listOf(
            // SY -->
            Preference.PreferenceItem.ListPreference(
                preference = libraryPreferences.groupLibraryUpdateType,
                title = stringResource(SYMR.strings.library_group_updates),
                entries = mapOf(
                    GroupLibraryMode.GLOBAL to stringResource(SYMR.strings.library_group_updates_global),
                    GroupLibraryMode.ALL_BUT_UNGROUPED to
                        stringResource(SYMR.strings.library_group_updates_all_but_ungrouped),
                    GroupLibraryMode.ALL to stringResource(SYMR.strings.library_group_updates_all),
                ),
            ),
            // SY <--
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.autoUpdateMetadata,
                title = stringResource(MR.strings.pref_library_update_refresh_metadata),
                subtitle = stringResource(MR.strings.pref_library_update_refresh_metadata_summary),
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                preference = libraryPreferences.autoUpdateMangaRestrictions,
                entries = mapOf(
                    MANGA_HAS_UNREAD to stringResource(MR.strings.pref_update_only_completely_read),
                    MANGA_NON_READ to stringResource(MR.strings.pref_update_only_started),
                    MANGA_NON_COMPLETED to stringResource(MR.strings.pref_update_only_non_completed),
                    MANGA_OUTSIDE_RELEASE_PERIOD to stringResource(MR.strings.pref_update_only_in_release_period),
                ),
                title = stringResource(MR.strings.pref_library_update_smart_update),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = libraryPreferences.newShowUpdatesCount,
                title = stringResource(MR.strings.pref_library_update_show_tab_badge),
            ),
        )
    }

    @Composable
    private fun getBehaviorGroup(
        libraryPreferences: LibraryPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_behavior),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToStartAction,
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_start),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = libraryPreferences.swipeToEndAction,
                    entries = mapOf(
                        LibraryPreferences.ChapterSwipeAction.Disabled to
                            stringResource(MR.strings.disabled),
                        LibraryPreferences.ChapterSwipeAction.ToggleBookmark to
                            stringResource(MR.strings.action_bookmark),
                        LibraryPreferences.ChapterSwipeAction.ToggleRead to
                            stringResource(MR.strings.action_mark_as_read),
                        LibraryPreferences.ChapterSwipeAction.Download to
                            stringResource(MR.strings.action_download),
                    ),
                    title = stringResource(MR.strings.pref_chapter_swipe_end),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = libraryPreferences.markDuplicateReadChapterAsRead,
                    entries = mapOf(
                        MARK_DUPLICATE_CHAPTER_READ_EXISTING to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_existing),
                        MARK_DUPLICATE_CHAPTER_READ_NEW to
                            stringResource(MR.strings.pref_mark_duplicate_read_chapter_read_new),
                    ),
                    title = stringResource(MR.strings.pref_mark_duplicate_read_chapter_read),
                ),
            ),
        )
    }

    // SY -->
    @Composable
    fun getSortingCategory(navigator: Navigator, libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        val tagCount by libraryPreferences.sortTagsForLibrary.collectAsState()
        return Preference.PreferenceGroup(
            stringResource(SYMR.strings.pref_sorting_settings),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(SYMR.strings.pref_tag_sorting),
                    subtitle = pluralStringResource(SYMR.plurals.pref_tag_sorting_desc, tagCount.size, tagCount.size),
                    onClick = {
                        navigator.push(SortTagScreen())
                    },
                ),
            ),
        )
    }
    // SY <--
}
