package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The badge and category layer of [LibraryPreferences], between the chapter
 * defaults and the display/filter layer. Split out for the 250-line cap.
 */
public open class LibraryCategoryPreferences(
    private val preferenceStore: PreferenceStore,
) : LibraryChapterPreferences(preferenceStore) {

    // region Badges

    /** Whether covers show a downloaded-chapter count badge. */
    public val downloadBadge: Preference<Boolean> = preferenceStore.getBoolean("display_download_badge", false)

    /** Whether covers show an unread-chapter count badge. */
    public val unreadBadge: Preference<Boolean> = preferenceStore.getBoolean("display_unread_badge", true)

    /** Whether local-source entries show a badge. */
    public val localBadge: Preference<Boolean> = preferenceStore.getBoolean("display_local_badge", true)

    /** Whether covers show the source language. */
    public val languageBadge: Preference<Boolean> = preferenceStore.getBoolean("display_language_badge", false)

    /** Whether the updates tab shows a count of unseen updates. */
    public val newShowUpdatesCount: Preference<Boolean> = preferenceStore.getBoolean("library_show_updates_count", true)

    /** Number of updates not yet seen; app state, not a user setting. */
    public val newUpdatesCount: Preference<Int> = preferenceStore.getInt(
        Preference.appStateKey("library_unseen_updates_count"),
        0,
    )

    // endregion

    // region Category

    /** Category new library entries go to; -1 asks every time. */
    public val defaultCategory: Preference<Int> = preferenceStore.getInt(
        LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY,
        -1,
    )

    /** Category tab shown when the library was last open. */
    public val lastUsedCategory: Preference<Int> = preferenceStore.getInt(
        Preference.appStateKey("last_used_category"),
        0,
    )

    /** Whether the library shows one tab per category. */
    public val categoryTabs: Preference<Boolean> = preferenceStore.getBoolean("display_category_tabs", true)

    /** Whether each category tab shows its entry count. */
    public val categoryNumberOfItems: Preference<Boolean> = preferenceStore.getBoolean("display_number_of_items", false)

    /** Whether display settings are stored per category rather than globally. */
    public val categorizedDisplaySettings: Preference<Boolean> =
        preferenceStore.getBoolean("categorized_display", false)

    /** Category ids library updates are limited to (empty = all). */
    public val updateCategories: Preference<Set<String>> = preferenceStore.getStringSet(
        LibraryPreferences.LIBRARY_UPDATE_CATEGORIES_PREF_KEY,
        emptySet(),
    )

    /** Category ids library updates skip. */
    public val updateCategoriesExclude: Preference<Set<String>> = preferenceStore.getStringSet(
        LibraryPreferences.LIBRARY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY,
        emptySet(),
    )

    // endregion
}
